/* Uređivanje tekstova javne stranice direktno na stranici (samo urednik/admin). */
(function () {
    var traka = document.getElementById('urednikTraka');
    if (!traka) return;

    var btnUredi = document.getElementById('utUredi');
    var btnSacuvaj = document.getElementById('utSacuvaj');
    var btnOdustani = document.getElementById('utOdustani');
    var poruka = document.getElementById('utPoruka');
    var polja = Array.prototype.slice.call(document.querySelectorAll('[data-uredi]'));
    var original = new Map();
    var uredjuje = false;

    document.body.classList.add('ima-urednik-traku');

    if (polja.length === 0) {
        btnUredi.hidden = true;
        poruka.textContent = 'Na ovoj stranici nema tekstova za uređivanje.';
        return;
    }

    function tekst(el) { return el.innerText.replace(/\u00A0/g, ' ').trim(); }

    function postaviEditabilno(el, da) {
        if (!da) { el.removeAttribute('contenteditable'); return; }
        try { el.contentEditable = 'plaintext-only'; } catch (e) { /* stariji preglednik */ }
        if (el.contentEditable !== 'plaintext-only') el.contentEditable = 'true';
    }

    function izmjene() {
        var m = {};
        polja.forEach(function (el) {
            var t = tekst(el);
            if (t !== original.get(el)) m[el.dataset.uredi] = t;
        });
        return m;
    }

    function brojIzmjena() { return Object.keys(izmjene()).length; }

    function osvjeziPoruku() {
        var n = brojIzmjena();
        poruka.textContent = n === 0
            ? 'Kliknite na tekst s isprekidanim okvirom. Obrisan tekst se vraća na original.'
            : 'Izmijenjeno tekstova: ' + n + ' — ne zaboravite sačuvati.';
        btnSacuvaj.disabled = n === 0;
    }

    function ukljuci() {
        uredjuje = true;
        document.body.classList.add('uredjivanje');
        polja.forEach(function (el) {
            original.set(el, tekst(el));
            postaviEditabilno(el, true);
        });
        btnUredi.hidden = true;
        btnOdustani.hidden = false;
        btnSacuvaj.hidden = false;
        osvjeziPoruku();
    }

    function iskljuci(vrati) {
        if (vrati) polja.forEach(function (el) { el.innerText = original.get(el); });
        uredjuje = false;
        document.body.classList.remove('uredjivanje');
        polja.forEach(function (el) { postaviEditabilno(el, false); });
        btnUredi.hidden = false;
        btnOdustani.hidden = true;
        btnSacuvaj.hidden = true;
        poruka.textContent = 'Kliknite "Uredi" pa direktno mijenjajte tekst na stranici.';
    }

    function sacuvaj() {
        var m = izmjene();
        if (Object.keys(m).length === 0) return;

        var headers = { 'Content-Type': 'application/json' };
        var token = document.querySelector('meta[name="_csrf"]');
        var header = document.querySelector('meta[name="_csrf_header"]');
        if (token && header && token.content) headers[header.content] = token.content;

        btnSacuvaj.disabled = true;
        poruka.textContent = 'Snimam…';

        fetch(traka.dataset.url, { method: 'POST', headers: headers, body: JSON.stringify(m) })
            .then(function (r) {
                if (!r.ok) return r.json().catch(function () { return {}; })
                    .then(function (j) { throw new Error(j.message || ('Greška ' + r.status)); });
                return r.json();
            })
            .then(function () {
                window.removeEventListener('beforeunload', upozori);
                location.reload();               // prazni tekstovi se vraćaju na original
            })
            .catch(function (e) {
                poruka.textContent = 'Nije sačuvano: ' + e.message;
                btnSacuvaj.disabled = false;
            });
    }

    function upozori(e) {
        if (uredjuje && brojIzmjena() > 0) { e.preventDefault(); e.returnValue = ''; }
    }

    // dok se uređuje, linkovi i dugmad na stranici ne vode nigdje
    document.addEventListener('click', function (e) {
        if (!uredjuje || traka.contains(e.target)) return;
        if (e.target.closest('a, button')) e.preventDefault();
    }, true);

    // Enter u naslovima/dugmadima ne pravi novi red
    document.addEventListener('keydown', function (e) {
        if (!uredjuje || e.key !== 'Enter') return;
        var el = e.target.closest && e.target.closest('[data-uredi]');
        if (el && !el.hasAttribute('data-uredi-vise')) e.preventDefault();
    });

    // lijepljenje uvijek kao običan tekst
    document.addEventListener('paste', function (e) {
        if (!uredjuje || !e.target.closest || !e.target.closest('[data-uredi]')) return;
        e.preventDefault();
        var t = (e.clipboardData || window.clipboardData).getData('text/plain');
        document.execCommand('insertText', false, t);
    });

    document.addEventListener('input', function () { if (uredjuje) osvjeziPoruku(); });
    window.addEventListener('beforeunload', upozori);

    btnUredi.addEventListener('click', ukljuci);
    btnOdustani.addEventListener('click', function () { iskljuci(true); });
    btnSacuvaj.addEventListener('click', sacuvaj);
})();