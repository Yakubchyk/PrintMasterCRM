/* PrintMaster CRM: диалоги, подтверждения, черновики, показ расчётов по выбранным работам */
(function () {
	'use strict';

	function qsa(sel, root) { return Array.prototype.slice.call((root || document).querySelectorAll(sel)); }
	function store(op, key, val) {
		try {
			if (op === 'get') return sessionStorage.getItem(key);
			if (op === 'set') sessionStorage.setItem(key, val);
			if (op === 'del') sessionStorage.removeItem(key);
		} catch (e) {}
		return null;
	}

	// --- подтверждение опасных действий ---
	document.addEventListener('submit', function (ev) {
		var msg = ev.target.getAttribute && ev.target.getAttribute('data-confirm');
		if (msg && !window.confirm(msg)) { ev.preventDefault(); }
	}, true);

	// --- выбор значения сразу отправляет форму (статус заказа) ---
	qsa('select[data-autosubmit]').forEach(function (s) {
		s.addEventListener('change', function () { s.form.submit(); });
	});

	// --- черновики форм в диалогах ---
	function draftOf(form) {
		var key = form.getAttribute('data-draft');
		if (!key) { return; }
		function save() {
			var d = {};
			qsa('input,select,textarea', form).forEach(function (e) {
				if (e.name && e.type !== 'hidden' && e.name !== 'pm_nonce') { d[e.name] = e.value; }
			});
			store('set', key, JSON.stringify(d));
		}
		form.addEventListener('input', save);
		form.addEventListener('submit', function () { store('del', key); });
		form._pmRestore = function () {
			try {
				var d = JSON.parse(store('get', key) || 'null');
				if (d) { qsa('input,select,textarea', form).forEach(function (e) { if (e.name && d[e.name] != null && e.type !== 'hidden') { e.value = d[e.name]; } }); }
			} catch (e) {}
		};
	}
	qsa('form[data-draft]').forEach(draftOf);

	// --- диалоги ---
	function openDialog(dlg) {
		qsa('form[data-draft]', dlg).forEach(function (f) { if (f._pmRestore) { f._pmRestore(); } });
		if (dlg.showModal) { dlg.showModal(); } else { dlg.setAttribute('open', ''); }
	}
	qsa('[data-pm-open]').forEach(function (b) {
		b.addEventListener('click', function () {
			var dlg = document.getElementById(b.getAttribute('data-pm-open'));
			if (dlg) { openDialog(dlg); }
		});
	});
	qsa('[data-pm-close]').forEach(function (b) {
		b.addEventListener('click', function () { var d = b.closest('dialog'); if (d) { d.close(); } });
	});
	qsa('dialog').forEach(function (d) {
		d.addEventListener('click', function (e) { if (e.target === d) { d.close(); } });
		// после ошибки сохранения открыть окно снова
		if (d.hasAttribute('data-open-on-error') && document.querySelector('.pm-app .alert.error')) { openDialog(d); }
	});

	// --- склад: показывать только поля выбранного раздела ---
	qsa('select[data-toggle-groups]').forEach(function (sel) {
		var dlg = sel.closest('dialog') || document;
		function toggle() {
			var c = sel.value;
			qsa('[data-for]', dlg).forEach(function (g) {
				var on = g.getAttribute('data-for').split(' ').indexOf(c) >= 0;
				g.style.display = on ? '' : 'none';
				qsa('input,select', g).forEach(function (i) { i.disabled = !on; });
			});
			var hint = document.getElementById('foilHint');
			if (hint) { hint.hidden = c !== 'FOIL'; }
		}
		sel.addEventListener('change', toggle);
		toggle();
	});

	// --- новый заказ: сначала выбор работ ---
	var works = document.getElementById('works');
	if (works) {
		var cid = works.getAttribute('data-customer');
		var key = 'works-' + cid;
		var draftKey = 'draft-' + cid;
		var boxes = qsa('input', works);
		var sections = qsa('section[data-work]');
		var orderSection = document.getElementById('orderSection');
		var hint = document.getElementById('worksHint');
		var product = document.getElementById('product');
		var form = document.getElementById('orderForm');
		var warn = document.getElementById('noCalc');

		var apply = function () {
			var list = boxes.filter(function (b) { return b.checked; }).map(function (b) { return b.value; });
			sections.forEach(function (s) {
				var on = list.indexOf(s.getAttribute('data-work')) >= 0;
				s.hidden = !on;
				// расчёты скрытой работы не должны попасть в заказ
				qsa('input[form="orderForm"]', s).forEach(function (c) { c.disabled = !on; if (!on) { c.checked = false; } });
			});
			orderSection.hidden = list.length === 0;
			hint.hidden = list.length > 0;
			store('set', key, JSON.stringify(list));
		};

		// описание заказа не теряется ни при ошибке, ни при добавлении расчёта (страница перезагружается)
		product.value = store('get', draftKey) || product.value;
		product.addEventListener('input', function () { store('set', draftKey, product.value); });
		form.addEventListener('submit', function (ev) {
			var checked = qsa('input[form="orderForm"]:checked').filter(function (c) { return !c.disabled; });
			if (checked.length === 0) {
				ev.preventDefault();
				warn.hidden = false;
				warn.scrollIntoView({ behavior: 'smooth', block: 'center' });
			} else {
				warn.hidden = true;
			}
		});
		try {
			var saved = JSON.parse(store('get', key) || '[]');
			boxes.forEach(function (b) { b.checked = saved.indexOf(b.value) >= 0; });
		} catch (e) {}
		boxes.forEach(function (b) { b.addEventListener('change', apply); });
		apply();
	}

	// --- запоминаем вид списка заказов (по клиентам / все) ---
	qsa('.seg a[data-view]').forEach(function (a) {
		a.addEventListener('click', function () { try { localStorage.setItem('pmOrdersView', a.getAttribute('data-view')); } catch (e) {} });
	});
	if (document.querySelector('.p-orders .seg')) {
		try {
			var p = new URLSearchParams(location.search);
			if (p.get('page') === 'pm-orders' && !p.has('view') && !p.has('q') && !p.has('order') && !p.has('customer') && localStorage.getItem('pmOrdersView') === 'all') {
				p.set('view', 'all');
				location.replace(location.pathname + '?' + p.toString());
			}
		} catch (e) {}
	}
})();
