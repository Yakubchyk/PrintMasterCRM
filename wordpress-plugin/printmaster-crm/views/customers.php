<?php
if ( ! defined( 'ABSPATH' ) ) {
	exit;
}
require_once __DIR__ . '/partials.php';

$cid = isset( $_GET['id'] ) ? (int) $_GET['id'] : 0; // phpcs:ignore WordPress.Security.NonceVerification
if ( $cid ) {
	$customer = PM_Customers::get( $cid );
	$orders   = PM_Orders::find( $cid );
	?>
<main class="p-customers">
	<p><a href="<?php echo esc_url( PM_View::url( 'pm-customers' ) ); ?>">← Все клиенты</a></p>
	<h1><?php echo pm_h( $customer->name ); ?></h1>

	<section>
		<?php pm_client_contact( $customer ); ?>
		<details style="margin-top:14px">
			<summary>Изменить данные клиента</summary>
			<?php echo PM_View::form_open( 'customer_update', array( 'id' => $customer->id ) ); // phpcs:ignore WordPress.Security.EscapeOutput ?>
				<?php pm_client_fields( $customer ); ?>
				<p><button type="submit">Сохранить</button></p>
			</form>
			<?php echo PM_View::form_open( 'customer_delete', array( 'id' => $customer->id ), 'data-confirm="Удалить клиента вместе со всеми его расчётами и заказами?"' ); // phpcs:ignore WordPress.Security.EscapeOutput ?>
				<button type="submit" class="danger">Удалить клиента</button>
			</form>
		</details>
	</section>

	<section>
		<h2>Заказы клиента</h2>
		<?php if ( ! $orders ) : ?>
			<div class="muted">Заказов пока нет</div>
		<?php else : ?>
			<div class="table-wrap"><table>
				<thead><tr><th>№</th><th>Дата</th><th>Описание</th><th class="num">Сумма</th><th>Статус</th></tr></thead>
				<tbody>
				<?php foreach ( $orders as $o ) : ?>
					<tr>
						<td><a class="olink order-id" href="<?php echo esc_url( PM_View::url( 'pm-orders', array( 'order' => $o->id ) ) ); ?>"><?php echo (int) $o->id; ?></a></td>
						<td><?php echo pm_h( pm_date( $o->created_at ) ); ?></td>
						<td><?php echo pm_h( $o->product ); ?></td>
						<td class="num total"><?php echo pm_h( PM_Num::money( $o->total_cost ) ); ?></td>
						<td><?php echo pm_status_badge( $o->status ); // phpcs:ignore WordPress.Security.EscapeOutput ?></td>
					</tr>
				<?php endforeach; ?>
				</tbody>
			</table></div>
		<?php endif; ?>
		<p>
			<a class="btn" href="<?php echo esc_url( PM_View::url( 'pm-new-order', array( 'customer' => $customer->id ) ) ); ?>">+ Новый заказ</a>
			<a class="btn secondary" href="<?php echo esc_url( PM_View::url( 'pm-orders', array( 'customer' => $customer->id ) ) ); ?>">Подробности заказов</a>
		</p>
	</section>
</main>
	<?php
	return;
}

$q         = isset( $_GET['q'] ) ? trim( sanitize_text_field( wp_unslash( $_GET['q'] ) ) ) : ''; // phpcs:ignore WordPress.Security.NonceVerification
$customers = PM_Customers::all( $q );
?>
<main class="p-customers">
	<h1>Клиенты</h1>

	<section>
		<div class="toolbar">
			<form method="get" action="<?php echo esc_url( admin_url( 'admin.php' ) ); ?>" class="row search">
				<input type="hidden" name="page" value="pm-customers">
				<div>
					<label for="pm-q">Поиск по названию, телефону или email</label>
					<input type="search" id="pm-q" name="q" value="<?php echo pm_a( $q ); ?>" placeholder="Начните вводить…">
				</div>
				<div class="fixed">
					<button type="submit" class="secondary">Найти</button>
					<?php if ( $q !== '' ) : ?><a class="btn secondary" href="<?php echo esc_url( PM_View::url( 'pm-customers' ) ); ?>">Сбросить</a><?php endif; ?>
				</div>
			</form>
			<button type="button" data-pm-open="newClient">+ Новый клиент</button>
		</div>

		<?php if ( ! $customers ) : ?>
			<div class="muted" style="margin-top:16px">Клиентов нет</div>
		<?php else : ?>
			<div class="table-wrap"><table class="clients">
				<thead><tr><th>Организация</th><th>Email</th><th>Телефон</th><th>Город</th></tr></thead>
				<tbody>
				<?php foreach ( $customers as $c ) : ?>
					<tr>
						<td><a class="cust-name" href="<?php echo esc_url( PM_View::url( 'pm-customers', array( 'id' => $c->id ) ) ); ?>"><span class="avatar"><?php echo pm_h( mb_strtoupper( mb_substr( $c->name, 0, 1 ) ) ); ?></span><span><?php echo pm_h( $c->name ); ?></span></a></td>
						<td><?php echo pm_h( $c->email ); ?></td>
						<td><?php echo pm_h( $c->phone ); ?></td>
						<td><?php echo pm_h( $c->city ); ?></td>
					</tr>
				<?php endforeach; ?>
				</tbody>
			</table></div>
		<?php endif; ?>
	</section>

	<dialog id="newClient" data-open-on-error>
		<?php echo PM_View::form_open( 'customer_create', array(), 'data-draft="newClientDraft"' ); // phpcs:ignore WordPress.Security.EscapeOutput ?>
			<div class="dlg-head"><h2>Новый клиент</h2></div>
			<div class="dlg-body">
				<?php pm_client_fields( null ); ?>
				<div class="dlg-actions">
					<button type="button" class="secondary" data-pm-close>Отмена</button>
					<button type="submit">Добавить клиента</button>
				</div>
			</div>
		</form>
	</dialog>
</main>
