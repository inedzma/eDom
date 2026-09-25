/* =========================================================
   E-Dom – iskačuće obavijesti (toast) u uglu ekrana
   - poruke sa servera (Thymeleaf) su već u HTML-u, ovaj fajl ih
     samo animira, gasi nakon par sekundi i dodaje dugme X
   - iz JavaScripta: toast('uspjeh', 'Sačuvano!')
                     toast('greska', 'Nešto nije u redu')
                     toast('upozorenje', 'Pažnja...')
   ========================================================= */
(function () {
    const TRAJANJE = 5000; // ms – koliko dugo ostaje na ekranu

    const IKONE = {
        uspjeh: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M8 12l3 3 5-6"/></svg>',
        greska: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M12 8v4M12 16h.01"/></svg>',
        upozorenje: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><path d="M12 9v4M12 17h.01"/></svg>'
    };
    const NASLOVI = { uspjeh: 'Uspješno', greska: 'Greška', upozorenje: 'Pažnja' };

    function kontejner() {
        let c = document.getElementById('toasts');
        if (!c) {
            c = document.createElement('div');
            c.id = 'toasts';
            c.className = 'toasts';
            document.body.appendChild(c);
        }
        return c;
    }

    function zatvori(t) {
        if (t.classList.contains('izlaz')) return;
        t.classList.add('izlaz');
        t.addEventListener('animationend', () => t.remove(), { once: true });
    }

    // doda X dugme, traku napretka i automatsko gašenje
    function pokreni(t) {
        const tip = t.dataset.tip || 'uspjeh';
        t.setAttribute('role', tip === 'greska' ? 'alert' : 'status');

        if (!t.querySelector('.toast-ikona')) {
            t.insertAdjacentHTML('afterbegin', '<span class="toast-ikona">' + IKONE[tip] + '</span>');
        }
        const x = document.createElement('button');
        x.type = 'button';
        x.className = 'toast-x';
        x.setAttribute('aria-label', 'Zatvori');
        x.innerHTML = '&times;';
        x.addEventListener('click', () => zatvori(t));
        t.appendChild(x);

        const traka = document.createElement('span');
        traka.className = 'toast-traka';
        traka.style.animationDuration = TRAJANJE + 'ms';
        t.appendChild(traka);

        let timer = setTimeout(() => zatvori(t), TRAJANJE);
        // pauza dok je miš iznad
        t.addEventListener('mouseenter', () => { clearTimeout(timer); t.classList.add('pauza'); });
        t.addEventListener('mouseleave', () => { t.classList.remove('pauza'); timer = setTimeout(() => zatvori(t), 2000); });
    }

    // globalna funkcija za pozivanje iz JS-a
    window.toast = function (tip, tekst) {
        const t = document.createElement('div');
        t.className = 'toast toast--' + tip;
        t.dataset.tip = tip;
        t.innerHTML = '<div class="toast-tekst"><b></b><span></span></div>';
        t.querySelector('b').textContent = NASLOVI[tip] || '';
        t.querySelector('span').textContent = tekst;
        kontejner().appendChild(t);
        pokreni(t);
    };

    // poruke koje je poslao server
    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('#toasts .toast').forEach(pokreni);

        // makni ?error, ?registrovan... iz adrese da se poruka ne ponovi na refresh
        if (location.search && document.querySelector('#toasts .toast')) {
            history.replaceState(null, '', location.pathname);
        }
    });
})();