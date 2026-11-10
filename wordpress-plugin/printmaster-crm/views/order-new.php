<?php
if ( ! defined( 'ABSPATH' ) ) {
	exit;
}
require_once __DIR__ . '/partials.php';

$cid = isset( $_GET['customer'] ) ? (int) $_GET['customer'] : 0; // phpcs:ignore WordPress.Security.NonceVerification
?>
<main class="p-neworder">
	<h1>Новый заказ</h1>

<?php if ( ! $cid ) : ?>
	<?php $customers = PM_Customers::all(); ?>
	<section>
		<form method="get" action="<?php echo esc_url( admin_url( 'admin.php' ) ); ?>" class="row">
			<input type="hidden" name="page" value="pm-new-order">
			<div><label for="pm-customer">Клиент</label>
				<select id="pm-customer" name="customer" required>
					<option value="" disabled selected>— выберите клиента —</option>
					<?php foreach ( $customers as $c ) : ?>
						<option value="<?php echo (int) $c->id; ?>"><?php echo pm_h( $c->name ); ?></option>
					<?php endforeach; ?>
				</select></div>
			<div class="fixed"><button type="submit">Дальше</button></div>
		</form>
		<?php if ( ! $customers ) : ?>
			<p class="muted">Клиентов нет — сначала добавьте клиента в разделе «Клиенты».</p>
		<?php endif; ?>
	</section>
</main>
<?php
	return;
endif;

$customer   = PM_Customers::get( $cid );
$cliche     = PM_Calcs::cliche_list( $cid );
$postpress  = PM_Calcs::postpress_list( $cid );
$printing   = PM_Calcs::printing_list( $cid );
$mat_names  = array();
$plates     = array_merge( PM_Materials::by_category( 'MAGNESIUM' ), PM_Materials::by_category( 'BRASS' ) );
$foils      = PM_Materials::by_category( 'FOIL' );
$papers     = PM_Materials::by_category( 'PAPER' );
foreach ( array_merge( $plates, $foils, $papers ) as $m ) {
	$mat_names[ (int) $m->id ] = $m->name;
}
$mat_name = function ( $id ) use ( $mat_names ) {
	return $id && isset( $mat_names[ (int) $id ] ) ? $mat_names[ (int) $id ] : '—';
};
$hid = array( 'customer_id' => $customer->id );
?>
	<p><a href="<?php echo esc_url( PM_View::url( 'pm-orders', array( 'customer' => $customer->id ) ) ); ?>">← Заказы клиента</a></p>
	<h2 style="margin-top:0"><?php echo pm_h( $customer->name ); ?></h2>

	<section>
		<h2>1. Какие работы заказывает клиент?</h2>
		<div class="works" id="works" data-customer="<?php echo (int) $customer->id; ?>">
			<label><input type="checkbox" value="CLICHE"> Клише</label>
			<label><input type="checkbox" value="POSTPRESS"> Тиснение</label>
			<label><input type="checkbox" value="PRINTING"> Печать</label>
		</div>
		<p class="muted" id="worksHint">Отметьте работы — появятся нужные расчёты.</p>
	</section>

	<!-- Отметки расчётов в таблицах относятся к форме заказа ниже (атрибут form="orderForm") -->

	<section data-work="CLICHE" hidden>
		<h2>Клише</h2>
		<?php echo PM_View::form_open( 'calc_add', $hid + array( 'kind' => 'cliche' ), 'class="row"' ); // phpcs:ignore WordPress.Security.EscapeOutput ?>
			<div><label>Ширина, <?php echo pm_h( $ul ); ?></label><input type="number" name="width" step="any" min="0" required></div>
			<div><label>Длина, <?php echo pm_h( $ul ); ?></label><input type="number" name="length" step="any" min="0" required></div>
			<div><label>Тех. поля (с каждой стороны), <?php echo pm_h( $ul ); ?></label><input type="number" name="margin" step="any" min="0" value="0"></div>
			<div><label>Количество клише</label><input type="number" name="quantity" min="1" step="1" value="1" required></div>
			<div><label>Материал клише</label>
				<select name="material_id">
					<option value="">— не списывать —</option>
					<?php foreach ( $plates as $m ) : ?>
						<option value="<?php echo (int) $m->id; ?>"><?php echo pm_h( PM_Units::cat_label( $m->category ) . ': ' . $m->name . ' (свободно ' . PM_Num::qty( PM_Materials::available( $m ) ) . ' см²)' ); ?></option>
					<?php endforeach; ?>
				</select></div>
			<div class="fixed"><button type="submit">Рассчитать</button></div>
		</form>
		<p class="muted" style="margin:4px 0">Цена считается по общему размеру (размер + поля). Материал резервируется на складе при оформлении заказа: общая площадь × количество.</p>
		<?php if ( $cliche ) : ?>
		<div class="table-wrap"><table>
			<thead><tr><th></th><th>Дата</th><th>Размер</th><th>Общий размер, площадь с полями</th><th>Материал</th><th class="num">Кол-во</th><th class="num">Стоимость</th><th></th></tr></thead>
			<tbody>
			<?php foreach ( $cliche as $p ) : ?>
				<tr>
					<td><input type="checkbox" name="cliche_ids[]" value="<?php echo (int) $p->id; ?>" form="orderForm"></td>
					<td><?php echo pm_h( pm_date( $p->created_at, true ) ); ?></td>
					<td>
						<span><?php echo pm_h( PM_Units::fmt_len( $p->width_cm, $lu ) . ' × ' . PM_Units::fmt_len( $p->length_cm, $lu ) . ' ' . $ul ); ?></span>
						<?php if ( (float) $p->margin_cm > 0 ) : ?><span class="muted">+ поля <?php echo pm_h( PM_Units::fmt_len( $p->margin_cm, $lu ) ); ?></span><?php endif; ?>
					</td>
					<td><b><?php echo pm_h( PM_Num::plain( PM_Calcs::cliche_area( $p ), 2 ) ); ?> см²</b></td>
					<td><?php echo pm_h( $mat_name( $p->material_id ) ); ?></td>
					<td class="num"><?php echo (int) $p->quantity; ?></td>
					<td class="num total"><?php echo pm_h( PM_Num::money( $p->total_price ) ); ?></td>
					<td class="num">
						<?php echo PM_View::form_open( 'calc_delete', $hid + array( 'kind' => 'cliche', 'calc_id' => $p->id ), 'class="inline"' ); // phpcs:ignore WordPress.Security.EscapeOutput ?>
							<button type="submit" class="danger small" title="Удалить расчёт">✕</button>
						</form>
					</td>
				</tr>
			<?php endforeach; ?>
			</tbody>
		</table></div>
		<?php endif; ?>
	</section>

	<section data-work="POSTPRESS" hidden>
		<h2>Тиснение</h2>
		<?php echo PM_View::form_open( 'calc_add', $hid + array( 'kind' => 'postpress' ), 'class="row"' ); // phpcs:ignore WordPress.Security.EscapeOutput ?>
			<div><label>Ширина, <?php echo pm_h( $ul ); ?></label><input type="number" name="width" step="any" min="0" required></div>
			<div><label>Длина, <?php echo pm_h( $ul ); ?></label><input type="number" name="length" step="any" min="0" required></div>
			<div><label>Тираж</label><input type="number" name="quantity" min="1" step="1" required></div>
			<div><label>Фольга</label>
				<select name="material_id">
					<option value="">— не списывать —</option>
					<?php foreach ( $foils as $m ) : ?>
						<option value="<?php echo (int) $m->id; ?>"><?php echo pm_h( $m->name . ' (свободно ' . PM_Num::qty( PM_Materials::available( $m ) ) . ' см²)' ); ?></option>
					<?php endforeach; ?>
				</select></div>
			<div class="fixed"><button type="submit">Рассчитать</button></div>
		</form>
		<p class="muted" style="margin:4px 0">Фольга резервируется при оформлении заказа: размер оттиска × тираж. Списывается, когда заказ станет «Выполнен».</p>
		<?php if ( $postpress ) : ?>
		<div class="table-wrap"><table>
			<thead><tr><th></th><th>Дата</th><th>Оттиск</th><th>Фольга</th><th class="num">Тираж</th><th class="num">Стоимость</th><th></th></tr></thead>
			<tbody>
			<?php foreach ( $postpress as $p ) : ?>
				<tr>
					<td><input type="checkbox" name="postpress_ids[]" value="<?php echo (int) $p->id; ?>" form="orderForm"></td>
					<td><?php echo pm_h( pm_date( $p->created_at, true ) ); ?></td>
					<td><?php echo pm_h( PM_Units::fmt_len( $p->width_cm, $lu ) . ' × ' . PM_Units::fmt_len( $p->length_cm, $lu ) . ' ' . $ul ); ?></td>
					<td><?php echo $p->material_id ? pm_h( $mat_name( $p->material_id ) . ' — ' . PM_Num::qty( PM_Calcs::postpress_foil_area( $p ) ) . ' см²' ) : '—'; ?></td>
					<td class="num"><?php echo (int) $p->quantity; ?></td>
					<td class="num total"><?php echo pm_h( PM_Num::money( $p->total_price ) ); ?></td>
					<td class="num">
						<?php echo PM_View::form_open( 'calc_delete', $hid + array( 'kind' => 'postpress', 'calc_id' => $p->id ), 'class="inline"' ); // phpcs:ignore WordPress.Security.EscapeOutput ?>
							<button type="submit" class="danger small" title="Удалить расчёт">✕</button>
						</form>
					</td>
				</tr>
			<?php endforeach; ?>
			</tbody>
		</table></div>
		<?php endif; ?>
	</section>

	<section data-work="PRINTING" hidden>
		<h2>Печать</h2>
		<?php echo PM_View::form_open( 'calc_add', $hid + array( 'kind' => 'printing' ), 'class="row"' ); // phpcs:ignore WordPress.Security.EscapeOutput ?>
			<div><label>Бумага</label>
				<select name="papier"><?php foreach ( PM_Units::PAPERS as $p ) : ?><option value="<?php echo pm_a( $p ); ?>"><?php echo pm_h( $p ); ?></option><?php endforeach; ?></select></div>
			<div><label>Цветность</label>
				<select name="colour"><?php foreach ( PM_Units::COLOURS as $c ) : ?><option value="<?php echo pm_a( $c ); ?>"><?php echo pm_h( $c ); ?></option><?php endforeach; ?></select></div>
			<div><label>Тираж</label><input type="number" name="quantity" min="1" step="1" required></div>
			<div class="fixed"><button type="submit">Рассчитать</button></div>
		</form>
		<?php if ( $printing ) : ?>
		<div class="table-wrap"><table>
			<thead><tr><th></th><th>Дата</th><th>Бумага</th><th>Цветность</th><th class="num">Тираж</th><th class="num">Стоимость</th><th></th></tr></thead>
			<tbody>
			<?php foreach ( $printing as $p ) : ?>
				<tr>
					<td><input type="checkbox" name="printing_ids[]" value="<?php echo (int) $p->id; ?>" form="orderForm"></td>
					<td><?php echo pm_h( pm_date( $p->created_at, true ) ); ?></td>
					<td><?php echo pm_h( $p->papier ); ?></td>
					<td><?php echo pm_h( $p->colour ); ?></td>
					<td class="num"><?php echo (int) $p->quantity; ?></td>
					<td class="num total"><?php echo pm_h( PM_Num::money( $p->total_cost ) ); ?></td>
					<td class="num">
						<?php echo PM_View::form_open( 'calc_delete', $hid + array( 'kind' => 'printing', 'calc_id' => $p->id ), 'class="inline"' ); // phpcs:ignore WordPress.Security.EscapeOutput ?>
							<button type="submit" class="danger small" title="Удалить расчёт">✕</button>
						</form>
					</td>
				</tr>
			<?php endforeach; ?>
			</tbody>
		</table></div>
		<?php endif; ?>
	</section>

	<section id="orderSection" hidden>
		<h2>Оформить заказ</h2>
		<div class="alert error" id="noCalc" hidden>Отметьте галочкой хотя бы один расчёт выше — описание сохранено, ничего не потеряно.</div>
		<p class="muted" style="margin-top:0">Отметьте галочками расчёты выше, которые входят в заказ.</p>
		<?php echo PM_View::form_open( 'order_create', $hid, 'id="orderForm"' ); // phpcs:ignore WordPress.Security.EscapeOutput ?>
			<label for="product">Детали заказа</label>
			<textarea id="product" name="product" required maxlength="255" placeholder="Что именно заказывает клиент"></textarea>

			<?php if ( $papers ) : ?>
			<details style="margin-top:12px">
				<summary>Бумага и картон: сколько листов зарезервировать</summary>
				<?php foreach ( $papers as $m ) : ?>
					<div class="row">
						<div>
							<label><?php echo pm_h( $m->name . ( $m->sheet_size ? ' (' . $m->sheet_size . ')' : '' ) . ' — свободно ' . PM_Num::qty( PM_Materials::available( $m ) ) . ' лист.' ); ?></label>
							<input type="number" name="m_<?php echo (int) $m->id; ?>" step="1" min="0" placeholder="0">
						</div>
					</div>
				<?php endforeach; ?>
			</details>
			<?php endif; ?>
			<p class="muted">Материалы резервируются при оформлении заказа и списываются со склада, когда заказ получает статус «Выполнен».</p>
			<p><button type="submit">Оформить заказ</button></p>
		</form>
	</section>
</main>
