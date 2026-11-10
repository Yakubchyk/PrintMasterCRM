<?php
if ( ! defined( 'ABSPATH' ) ) {
	exit;
}
require_once __DIR__ . '/partials.php';

$order_id = isset( $_GET['order'] ) ? (int) $_GET['order'] : 0; // phpcs:ignore WordPress.Security.NonceVerification
$cust_id  = isset( $_GET['customer'] ) ? (int) $_GET['customer'] : 0; // phpcs:ignore WordPress.Security.NonceVerification

// ---------- один заказ ----------
if ( $order_id ) {
	$order    = PM_Orders::get( $order_id );
	$customer = PM_Customers::get( $order->customer_id );
	$mats     = PM_Orders::materials_by_order( array( $order ) );
	?>
<main class="p-orders">
	<p>
		<a href="<?php echo esc_url( PM_View::url( 'pm-orders', array( 'view' => 'all' ) ) ); ?>">← Все заказы</a>
		&nbsp;·&nbsp;
		<a href="<?php echo esc_url( PM_View::url( 'pm-orders', array( 'customer' => $customer->id ) ) ); ?>">Все заказы клиента: <?php echo pm_h( $customer->name ); ?></a>
	</p>
	<?php pm_order_card( $order, $customer, isset( $mats[ (int) $order->id ] ) ? $mats[ (int) $order->id ] : array(), true, 'order' ); ?>
</main>
	<?php
	return;
}

// ---------- заказы одного клиента ----------
if ( $cust_id ) {
	$customer = PM_Customers::get( $cust_id );
	$orders   = PM_Orders::find( $cust_id );
	$mats     = PM_Orders::materials_by_order( $orders );
	?>
<main class="p-orders">
	<p><a href="<?php echo esc_url( PM_View::url( 'pm-orders' ) ); ?>">← Все клиенты</a></p>
	<h1><?php echo pm_h( $customer->name ); ?></h1>

	<section>
		<?php pm_client_contact( $customer ); ?>
		<p style="margin-bottom:0">
			<a class="btn" href="<?php echo esc_url( PM_View::url( 'pm-new-order', array( 'customer' => $customer->id ) ) ); ?>">+ Новый заказ</a>
			<a class="btn secondary" href="<?php echo esc_url( PM_View::url( 'pm-customers', array( 'id' => $customer->id ) ) ); ?>">Карточка клиента</a>
		</p>
	</section>

	<section>
		<?php if ( ! $orders ) : ?>
			<div class="muted">Заказов пока нет</div>
		<?php endif; ?>
		<?php foreach ( $orders as $o ) : ?>
			<?php pm_order_card( $o, $customer, isset( $mats[ (int) $o->id ] ) ? $mats[ (int) $o->id ] : array(), false ); ?>
		<?php endforeach; ?>
	</section>
</main>
<script>try { sessionStorage.removeItem('draft-<?php echo (int) $customer->id; ?>'); } catch (e) {}</script>
	<?php
	return;
}

// ---------- список: по клиентам / все заказы ----------
$q    = isset( $_GET['q'] ) ? trim( sanitize_text_field( wp_unslash( $_GET['q'] ) ) ) : ''; // phpcs:ignore WordPress.Security.NonceVerification
$view = ( isset( $_GET['view'] ) && $_GET['view'] === 'all' ) ? 'all' : 'clients'; // phpcs:ignore WordPress.Security.NonceVerification
?>
<main class="p-orders">
	<h1>Заказы</h1>

	<section>
		<div class="toolbar">
			<form method="get" action="<?php echo esc_url( admin_url( 'admin.php' ) ); ?>" class="search row">
				<input type="hidden" name="page" value="pm-orders">
				<input type="hidden" name="view" value="<?php echo pm_a( $view ); ?>">
				<div>
					<label for="pm-q">Поиск: клиент, номер заказа, описание, материал…</label>
					<input type="search" id="pm-q" name="q" value="<?php echo pm_a( $q ); ?>">
				</div>
				<div class="fixed">
					<button type="submit">Найти</button>
					<?php if ( $q !== '' ) : ?><a class="btn secondary" href="<?php echo esc_url( PM_View::url( 'pm-orders', array( 'view' => $view ) ) ); ?>">Сбросить</a><?php endif; ?>
				</div>
			</form>
			<div class="toolbar-right">
				<div class="seg" title="Как показывать заказы">
					<a href="<?php echo esc_url( PM_View::url( 'pm-orders', array( 'view' => 'clients' ) ) ); ?>" class="<?php echo ( $view === 'clients' && $q === '' ) ? 'active' : ''; ?>" data-view="clients">По клиентам</a>
					<a href="<?php echo esc_url( PM_View::url( 'pm-orders', array( 'view' => 'all' ) ) ); ?>" class="<?php echo ( $view === 'all' || $q !== '' ) ? 'active' : ''; ?>" data-view="all">Все заказы</a>
				</div>
				<a class="btn" href="<?php echo esc_url( PM_View::url( 'pm-new-order' ) ); ?>">+ Новый заказ</a>
			</div>
		</div>
	</section>

	<?php if ( $q === '' && $view === 'clients' ) : ?>
		<?php $summaries = PM_Orders::summaries( PM_Customers::all() ); ?>
		<section>
			<?php if ( ! $summaries ) : ?>
				<div class="muted">Клиентов нет. Сначала добавьте клиента в разделе «Клиенты».</div>
			<?php else : ?>
				<div class="table-wrap"><table>
					<thead><tr><th>Клиент</th><th class="num">Заказов</th><th class="num">Не выполнено</th><th class="num">Сумма</th><th>Последний заказ</th></tr></thead>
					<tbody>
					<?php foreach ( $summaries as $s ) : ?>
						<tr>
							<td class="client-cell"><a href="<?php echo esc_url( PM_View::url( 'pm-orders', array( 'customer' => $s->customer->id ) ) ); ?>"><?php echo pm_h( $s->customer->name ); ?></a></td>
							<td class="num"><?php echo (int) $s->order_count; ?></td>
							<td class="num"><?php echo (int) $s->active_count; ?></td>
							<td class="num total"><?php echo pm_h( PM_Num::money( $s->total ) ); ?></td>
							<td class="muted"><?php echo $s->last_order_at ? pm_h( pm_date( $s->last_order_at ) ) : '—'; ?></td>
						</tr>
					<?php endforeach; ?>
					</tbody>
				</table></div>
			<?php endif; ?>
		</section>
	<?php else : ?>
		<?php
		$orders    = PM_Orders::search( $q );
		$mats      = PM_Orders::materials_by_order( $orders );
		$customers = array();
		foreach ( PM_Customers::all() as $c ) {
			$customers[ (int) $c->id ] = $c;
		}
		?>
		<section>
			<?php if ( $q !== '' ) : ?><h2>Найдено заказов: <?php echo (int) count( $orders ); ?></h2><?php endif; ?>
			<?php if ( ! $orders ) : ?>
				<div class="muted">Заказов нет</div>
			<?php else : ?>
				<div class="table-wrap"><table>
					<thead><tr><th>№</th><th>Дата</th><th>Описание</th><th class="num">Сумма</th><th>Статус</th><th>Клиент</th></tr></thead>
					<tbody>
					<?php foreach ( $orders as $o ) : ?>
						<?php
						$open = esc_url( PM_View::url( 'pm-orders', array( 'order' => $o->id ) ) );
						$cu   = isset( $customers[ (int) $o->customer_id ] ) ? $customers[ (int) $o->customer_id ] : null;
						?>
						<tr>
							<td><a class="olink order-id" href="<?php echo $open; // phpcs:ignore WordPress.Security.EscapeOutput ?>">№ <?php echo (int) $o->id; ?></a></td>
							<td><a class="olink" href="<?php echo $open; // phpcs:ignore WordPress.Security.EscapeOutput ?>"><?php echo pm_h( pm_date( $o->created_at ) ); ?></a></td>
							<td>
								<a class="olink" href="<?php echo $open; // phpcs:ignore WordPress.Security.EscapeOutput ?>">
									<span><?php echo pm_h( $o->product ); ?></span>
									<?php if ( isset( $mats[ (int) $o->id ] ) ) : ?>
										<div class="muted small-text"><?php
											$parts = array();
											foreach ( $mats[ (int) $o->id ] as $u ) {
												$parts[] = $u->material_name . ' — ' . PM_Num::qty( $u->quantity ) . ' ' . PM_Units::cat_unit( $u->material_category )['label'];
											}
											echo pm_h( implode( '; ', $parts ) );
										?></div>
									<?php endif; ?>
								</a>
							</td>
							<td class="num total"><?php echo pm_h( PM_Num::money( $o->total_cost ) ); ?></td>
							<td><?php pm_status_select( $o, array( 'back' => 'all', 'q' => $q ) ); ?></td>
							<td class="client-cell"><?php if ( $cu ) : ?><a href="<?php echo esc_url( PM_View::url( 'pm-orders', array( 'customer' => $cu->id ) ) ); ?>"><?php echo pm_h( $cu->name ); ?></a><?php endif; ?></td>
						</tr>
					<?php endforeach; ?>
					</tbody>
				</table></div>
			<?php endif; ?>
		</section>
	<?php endif; ?>
</main>
