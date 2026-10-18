/* =========================================================
   E-Dom — korak "Dokumenti"
   Prilaganje i brisanje bez ponovnog učitavanja stranice:
   forma se šalje u pozadini, pa se osvježi samo spisak, a
   stranica ostaje na istom mjestu.
   Bez JavaScripta forme i dalje rade na stari način.
   ========================================================= */
(function () {
    'use strict';

    const SADRZAJ = '#pd-sadrzaj';

    /* ---------------- popup ---------------- */
    const dialog = document.getElementById('pd-dialog');

    function pitaj(naslov, html, tekstDa, tekstNe) {
        if (!dialog || typeof dialog.showModal !== 'function') {
            const tmp = document.createElement('div');
            tmp.innerHTML = html;
            return Promise.resolve(window.confirm(naslov + '\n\n' + tmp.textContent.trim()));
        }
        dialog.querySelector('#pd-dialog-naslov').textContent = naslov;
        dialog.querySelector('#pd-dialog-tekst').innerHTML = html;
        dialog.querySelector('#pd-dialog-da').textContent = tekstDa;
        dialog.querySelector('#pd-dialog-ne').textContent = tekstNe;

        return new Promise(function (resolve) {
            function zatvori(odgovor) {
                dialog.close();
                da.removeEventListener('click', onDa);
                ne.removeEventListener('click', onNe);
                dialog.removeEventListener('cancel', onNe);
                resolve(odgovor);
            }
            const da = dialog.querySelector('#pd-dialog-da');
            const ne = dialog.querySelector('#pd-dialog-ne');
            const onDa = function () { zatvori(true); };
            const onNe = function (e) { if (e) e.preventDefault(); zatvori(false); };
            da.addEventListener('click', onDa);
            ne.addEventListener('click', onNe);
            dialog.addEventListener('cancel', onNe);
            dialog.showModal();
            ne.focus();
        });
    }

    function escape(s) {
        return s.replace(/[&<>"']/g, function (c) {
            return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
        });
    }

    /* ---------------- poruka iz odgovora servera ---------------- */
    // Server poruke šalje kao toast (flash atribut). Iz vraćene stranice
    // uzimamo tekst toasta i prikazujemo ga u redu u kojem je student radio.
    function porukaIz(doc) {
        const kandidati = Array.from(doc.querySelectorAll('[class*="toast"]'))
            .filter(function (el) { return !el.querySelector('[class*="toast"]'); })
            .filter(function (el) { return el.textContent.trim().length > 0; });
        if (!kandidati.length) return null;
        const el = kandidati[0];
        const greska = /err|gresk|grešk|danger|fail|warn/i.test(el.className);
        return { tekst: el.textContent.replace(/\s+/g, ' ').replace(/[×✕]\s*$/, '').trim(), greska: greska };
    }

    function prikaziPoruku(red, poruka) {
        if (!red || !poruka) return;
        let mjesto = red.querySelector('.pd-poruka');
        if (!mjesto) {
            mjesto = document.createElement('p');
            mjesto.className = 'pd-poruka';
            red.appendChild(mjesto);
        }
        mjesto.textContent = poruka.tekst;
        mjesto.classList.toggle('pd-poruka--greska', poruka.greska);
        mjesto.classList.add('pd-poruka--vidljiva');
        if (!poruka.greska) {
            red.classList.add('pd-red--bljesak');
            setTimeout(function () { red.classList.remove('pd-red--bljesak'); }, 1600);
            setTimeout(function () { mjesto.classList.remove('pd-poruka--vidljiva'); }, 4000);
        }
    }

    /* ---------------- slanje forme u pozadini ---------------- */
    async function posalji(form, uspjeh) {
        const red = form.closest('.pd-red');
        const idReda = red ? red.id : null;
        const dugme = form.querySelector('button[type=submit]');
        const staro = dugme ? dugme.innerHTML : '';

        // gdje je red na ekranu prije osvježavanja
        const prije = red ? red.getBoundingClientRect().top : null;

        if (dugme) {
            dugme.disabled = true;
            dugme.innerHTML = '<span class="spinner"></span>';
        }

        try {
            const res = await fetch(form.action, {
                method: 'POST',
                body: new FormData(form),
                credentials: 'same-origin'
            });
            const html = await res.text();
            let doc = new DOMParser().parseFromString(html, 'text/html');
            const poruka = porukaIz(doc);

            // ako server nije vratio ovu istu stranicu, učitaj je ponovo
            if (!doc.querySelector(SADRZAJ)) {
                const svjeze = await fetch(window.location.href, { credentials: 'same-origin' });
                doc = new DOMParser().parseFromString(await svjeze.text(), 'text/html');
            }
            const novi = doc.querySelector(SADRZAJ);
            if (!res.ok || !novi) throw new Error('server');

            document.querySelector(SADRZAJ).innerHTML = novi.innerHTML;

            const noviRed = idReda ? document.getElementById(idReda) : null;
            if (noviRed && prije !== null) {
                window.scrollBy(0, noviRed.getBoundingClientRect().top - prije);
            }
            prikaziPoruku(noviRed, poruka || { tekst: uspjeh, greska: false });

        } catch (e) {
            if (dugme) {
                dugme.disabled = false;
                dugme.innerHTML = staro;
            }
            prikaziPoruku(red, {
                tekst: 'Nije uspjelo. Provjerite vezu i veličinu fajla, pa pokušajte ponovo.',
                greska: true
            });
        }
    }

    document.addEventListener('submit', async function (e) {
        const form = e.target;
        if (!form.closest(SADRZAJ)) return;

        if (form.classList.contains('pd-upload')) {
            e.preventDefault();
            posalji(form, 'Dokument je priložen.');
        }

        if (form.classList.contains('pd-obrisi')) {
            e.preventDefault();
            const ok = await pitaj('Obrisati dokument?',
                'Dokument će biti uklonjen iz prijave. Možete ga ponovo priložiti kad god želite.',
                'Obriši', 'Odustani');
            if (ok) posalji(form, 'Dokument je obrisan.');
        }
    });

    /* ---------------- ime odabranog fajla ---------------- */
    document.addEventListener('change', function (e) {
        const input = e.target;
        if (!input.matches('.pd-fajl-izbor input[type=file]')) return;
        const izbor = input.closest('.pd-fajl-izbor');
        const ime = izbor.querySelector('.pd-fajl-ime');
        if (input.files && input.files.length) {
            ime.textContent = input.files[0].name;
            izbor.classList.add('pd-fajl-izbor--odabran');
        } else {
            ime.textContent = 'Odaberi fajl';
            izbor.classList.remove('pd-fajl-izbor--odabran');
        }
    });

    /* ---------------- "Dalje" kad nisu priloženi svi obavezni ---------------- */
    document.addEventListener('click', async function (e) {
        const dalje = e.target.closest('#pd-dalje');
        if (!dalje) return;

        const nedostaju = Array.from(
            document.querySelectorAll('#pd-obavezni .pd-red:not(.pd-red--ok) .pd-red-tekst b')
        ).map(function (b) { return b.textContent.trim(); });

        if (!nedostaju.length) return;   // sve je priloženo, idi dalje bez pitanja

        e.preventDefault();
        const ok = await pitaj(
            'Niste priložili sve obavezne dokumente',
            '<p>Još nedostaje:</p><ul>' +
            nedostaju.map(function (n) { return '<li>' + escape(n) + '</li>'; }).join('') +
            '</ul><p>Možete nastaviti i dokumente priložiti naknadno, dok je prijava otvorena. ' +
            'Jeste li sigurni da želite nastaviti?</p>',
            'Nastavi', 'Ostani i priloži');
        if (ok) window.location.href = dalje.href;
    });
})();