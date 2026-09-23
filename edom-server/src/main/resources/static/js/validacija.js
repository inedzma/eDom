function edomPoruka(el) {

    el.setCustomValidity('');
    if (el.validity.valid) return;

    const v = el.validity;
    let p = 'Unos nije ispravan.';

    if (v.valueMissing) {
        p = (el.tagName === 'SELECT')
            ? 'Odaberite jednu od ponuđenih opcija.'
            : 'Ovo polje je obavezno.';
    } else if (v.typeMismatch && el.type === 'email') {
        p = 'Unesite ispravnu e-mail adresu, npr. ime.prezime@primjer.ba';
    } else if (v.tooShort) {
        p = 'Unesite najmanje ' + el.minLength + ' znakova.';
    } else if (v.tooLong) {
        p = 'Dozvoljeno je najviše ' + el.maxLength + ' znakova.';
    } else if (v.rangeUnderflow) {
        p = 'Vrijednost ne može biti manja od ' + el.min + '.';
    } else if (v.rangeOverflow) {
        p = 'Vrijednost ne može biti veća od ' + el.max + '.';
    } else if (v.patternMismatch) {
        p = el.dataset.poruka || 'Uneseni format nije ispravan.';
    } else if (v.stepMismatch || v.badInput) {
        p = 'Unesite ispravnu vrijednost.';
    }

    el.setCustomValidity(p);
}

document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('input, select, textarea').forEach(function (el) {
        el.addEventListener('invalid', function () { edomPoruka(el); });
        el.addEventListener('input',  function () { el.setCustomValidity(''); });
        el.addEventListener('change', function () { el.setCustomValidity(''); });
    });
});