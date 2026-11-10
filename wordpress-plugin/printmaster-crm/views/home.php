<?php if ( ! defined( 'ABSPATH' ) ) { exit; } ?>
<main class="home">
	<h1>PrintMaster</h1>
	<p class="lead">Выберите раздел</p>
	<div class="tiles">
		<a class="tile" href="<?php echo esc_url( PM_View::url( 'pm-customers' ) ); ?>">
			<span class="icon i-blue"><svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="9" cy="8" r="3.2"/><path d="M3 20c0-3.3 2.7-6 6-6s6 2.7 6 6"/><circle cx="17" cy="9" r="2.4"/><path d="M17 14c2.5 0 4.5 2 4.5 4.5"/></svg></span>
			<span class="title">Клиенты</span>
			<span class="sub">Данные и контакты</span>
		</a>
		<a class="tile" href="<?php echo esc_url( PM_View::url( 'pm-orders' ) ); ?>">
			<span class="icon i-green"><svg viewBox="0 0 24 24" aria-hidden="true"><rect x="5" y="4" width="14" height="17" rx="2"/><path d="M9 4V3h6v1M9 10h6M9 14h6M9 18h3"/></svg></span>
			<span class="title">Заказы</span>
			<span class="sub">Список и поиск</span>
		</a>
		<a class="tile" href="<?php echo esc_url( PM_View::url( 'pm-new-order' ) ); ?>">
			<span class="icon i-orange"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z"/><path d="M14 3v5h5M12 11v6M9 14h6"/></svg></span>
			<span class="title">Новый заказ</span>
			<span class="sub">Расчёт и оформление</span>
		</a>
		<a class="tile" href="<?php echo esc_url( PM_View::url( 'pm-materials' ) ); ?>">
			<span class="icon i-purple"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 8l9-5 9 5v9l-9 5-9-5z"/><path d="M3 8l9 5 9-5M12 13v9"/></svg></span>
			<span class="title">Материалы</span>
			<span class="sub">Склад и остатки</span>
		</a>
		<a class="tile" href="<?php echo esc_url( PM_View::url( 'pm-settings' ) ); ?>">
			<span class="icon i-grey"><svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z"/></svg></span>
			<span class="title">Настройки</span>
			<span class="sub">Цены и единицы</span>
		</a>
	</div>
</main>
