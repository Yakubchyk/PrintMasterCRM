<?php
if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/** Поля клиента: название, email и телефон, ниже отдельным блоком адрес. $c может быть null (новый клиент). */
function pm_client_fields( $c = null ) {
	$v = function ( $prop ) use ( $c ) {
		return $c ? (string) $c->$prop : '';
	};
	?>
	<div class="client-form">
		<div>
			<label>Название организации</label>
			<input class="big" type="text" name="name" value="<?php echo pm_a( $v( 'name' ) ); ?>" required maxlength="255" autocomplete="off">
		</div>
		<div class="cf-contact">
			<div><label>Email</label><input type="email" name="email" value="<?php echo pm_a( $v( 'email' ) ); ?>" required></div>
			<div><label>Телефон</label><input type="text" name="phone" value="<?php echo pm_a( $v( 'phone' ) ); ?>" required></div>
		</div>
		<fieldset class="cf-address">
			<legend>Адрес</legend>
			<div class="cf-row1">
				<div><label>Индекс</label><input type="text" name="postal_code" value="<?php echo pm_a( $v( 'postal_code' ) ); ?>" maxlength="20"></div>
				<div><label>Город</label><input type="text" name="city" value="<?php echo pm_a( $v( 'city' ) ); ?>" maxlength="100"></div>
			</div>
			<div class="cf-row2">
				<div><label>Улица</label><input type="text" name="street" value="<?php echo pm_a( $v( 'street' ) ); ?>" maxlength="150"></div>
				<div><label>Дом</label><input type="text" name="house_number" value="<?php echo pm_a( $v( 'house_number' ) ); ?>" maxlength="20"></div>
			</div>
		</fieldset>
	</div>
	<?php
}

/** Показ клиента: основное — email и телефон, адрес отдельным блоком. */
function pm_client_contact( $c ) {
	$addr = PM_Customers::address_line( $c );
	?>
	<div class="contact">
		<div class="contact-main">
			<span><span class="muted">Email</span> <b><?php echo pm_h( $c->email ); ?></b></span>
			<span><span class="muted">Телефон</span> <b><?php echo pm_h( $c->phone ); ?></b></span>
		</div>
		<?php if ( $addr !== '' ) : ?>
			<div class="contact-addr"><span class="muted">Адрес</span> <b><?php echo pm_h( $addr ); ?></b></div>
		<?php endif; ?>
	</div>
	<?php
}

/** Выбор статуса заказа: форма, отправляется сразу при выборе. */
function pm_status_select( $o, array $extra = array() ) {
	echo PM_View::form_open( 'order_status', array_merge( array( 'id' => $o->id ), $extra ), 'class="inline"' ); // phpcs:ignore WordPress.Security.EscapeOutput
	?>
	<select name="status" class="status st-<?php echo pm_a( $o->status ); ?>" data-autosubmit aria-label="Статус заказа">
		<?php foreach ( PM_Units::STATUSES as $code => $label ) : ?>
			<option value="<?php echo pm_a( $code ); ?>" <?php selected( $o->status, $code ); ?>><?php echo pm_h( $label ); ?></option>
		<?php endforeach; ?>
	</select>
	</form>
	<?php
}

/** Развёрнутая карточка заказа. $items — материалы заказа, $show_customer — показывать клиента, $back — куда вернуться после смены статуса. */
function pm_order_card( $o, $customer, array $items, $show_customer, $back = '' ) {
	?>
	<article class="order" id="order-<?php echo (int) $o->id; ?>">
		<header>
			<div>
				<a class="order-id" style="font-size:17px" href="<?php echo esc_url( PM_View::url( 'pm-orders', array( 'order' => $o->id ) ) ); ?>">Заказ №<?php echo (int) $o->id; ?></a>
				<?php if ( $show_customer ) : ?>
					· <a href="<?php echo esc_url( PM_View::url( 'pm-orders', array( 'customer' => $customer->id ) ) ); ?>"><?php echo pm_h( $customer->name ); ?></a>
				<?php endif; ?>
			</div>
			<div class="order-actions">
				<?php pm_status_select( $o, array( 'back' => $back ) ); ?>
				<a class="btn secondary small" target="_blank" rel="noopener" href="<?php echo esc_url( admin_url( 'admin-post.php?action=pm_print&id=' . (int) $o->id ) ); ?>" title="Печать карточки заказа (половина A4, конверт для макета)">🖨 Карточка</a>
				<?php echo PM_View::form_open( 'order_delete', array( 'id' => $o->id ), 'class="inline" data-confirm="Удалить заказ? Резерв снимется, а уже списанные материалы вернутся на склад."' ); // phpcs:ignore WordPress.Security.EscapeOutput ?>
					<button type="submit" class="danger small" title="Удалить заказ">✕</button>
				</form>
			</div>
		</header>

		<div class="meta">
			<span><span class="muted">Создан</span> <b><?php echo pm_h( pm_date( $o->created_at, true ) ); ?></b></span>
			<?php if ( $o->completed_at ) : ?>
				<span><span class="muted">Выполнен</span> <b><?php echo pm_h( pm_date( $o->completed_at, true ) ); ?></b></span>
			<?php endif; ?>
			<span><span class="muted">Телефон</span> <b><?php echo pm_h( $customer->phone ); ?></b></span>
			<span><span class="muted">Email</span> <b><?php echo pm_h( $customer->email ); ?></b></span>
		</div>

		<div class="block">
			<div class="block-title">Описание</div>
			<div><?php echo pm_h( $o->product ); ?></div>
		</div>

		<?php if ( $o->details !== null && $o->details !== '' ) : ?>
			<div class="block">
				<div class="block-title">Состав работ</div>
				<div class="lines"><?php echo pm_h( $o->details ); ?></div>
			</div>
		<?php endif; ?>

		<?php if ( $items ) : ?>
			<div class="block">
				<div class="block-title">Материалы</div>
				<table class="mini">
					<thead><tr><th>Материал</th><th class="num">Количество</th><th>Склад</th></tr></thead>
					<tbody>
					<?php foreach ( $items as $u ) : ?>
						<tr>
							<td><?php echo pm_h( $u->material_name ); ?></td>
							<td class="num"><?php echo pm_h( PM_Num::qty( $u->quantity ) . ' ' . PM_Units::cat_unit( $u->material_category )['label'] ); ?></td>
							<td><span class="badge <?php echo (int) $u->written_off ? 'st-DONE' : 'st-IN_PROGRESS'; ?>"><?php echo (int) $u->written_off ? 'списано' : 'в резерве'; ?></span></td>
						</tr>
					<?php endforeach; ?>
					</tbody>
				</table>
			</div>
		<?php endif; ?>

		<div class="costs">
			<?php if ( (float) $o->cliche_price > 0 ) : ?><span>Клише: <b><?php echo pm_h( PM_Num::money( $o->cliche_price ) ); ?></b></span><?php endif; ?>
			<?php if ( (float) $o->postpress_price > 0 ) : ?><span>Тиснение: <b><?php echo pm_h( PM_Num::money( $o->postpress_price ) ); ?></b></span><?php endif; ?>
			<?php if ( (float) $o->printing_price > 0 ) : ?><span>Печать: <b><?php echo pm_h( PM_Num::money( $o->printing_price ) ); ?></b></span><?php endif; ?>
			<span class="grand">Итого: <b class="total"><?php echo pm_h( PM_Num::money( $o->total_cost ) ); ?></b></span>
		</div>
	</article>
	<?php
}
