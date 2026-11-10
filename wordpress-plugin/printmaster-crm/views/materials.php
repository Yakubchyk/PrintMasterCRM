<?php
if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

$mid = isset( $_GET['id'] ) ? (int) $_GET['id'] : 0; // phpcs:ignore WordPress.Security.NonceVerification

// ---------- один материал ----------
if ( $mid ) {
	$m        = PM_Materials::get( $mid );
	$unit     = PM_Materials::unit( $m );
	$area     = PM_Materials::sheet_area( $m );
	$avail    = PM_Materials::available( $m );
	$is_foil  = $m->category === 'FOIL';
	$is_plate = $m->category === 'MAGNESIUM' || $m->category === 'BRASS';
	$moves    = PM_Materials::movements( $mid );
	$step     = $unit['whole'] ? '1' : 'any';
	?>
<main class="p-materials">
	<p><a href="<?php echo esc_url( PM_View::url( 'pm-materials' ) ); ?>">← Все материалы</a></p>
	<h1><?php echo pm_h( $m->name ); ?></h1>

	<section>
		<div class="muted"><?php echo pm_h( PM_Units::cat_label( $m->category ) ); ?></div>
		<?php if ( $m->sheet_size ) : ?><div>Размер листа: <b><?php echo pm_h( $m->sheet_size ); ?></b></div><?php endif; ?>
		<?php if ( $m->foil_type ) : ?><div>Тип фольги: <b><?php echo pm_h( PM_Units::FOIL_TYPES[ $m->foil_type ] ); ?></b></div><?php endif; ?>
		<?php if ( $area !== null && $is_foil ) : ?>
			<div>Рулон: <b><?php echo pm_h( PM_Units::fmt_len( $m->sheet_width_cm, $lu ) . ' ' . $ul . ' × ' . PM_Num::plain( PM_Materials::roll_length_m( $m ) ) . ' м' ); ?></b>
				(<?php echo pm_h( PM_Num::qty( $area ) ); ?> см²)
				· свободно ≈ <b><?php echo pm_h( PM_Num::plain( floor( $avail / $area * 10 ) / 10, 1 ) ); ?></b> рулона</div>
		<?php elseif ( $area !== null ) : ?>
			<div>Исходный лист: <b><?php echo pm_h( PM_Units::fmt_len( $m->sheet_width_cm, $lu ) . ' × ' . PM_Units::fmt_len( $m->sheet_length_cm, $lu ) . ' ' . $ul ); ?></b>
				(<?php echo pm_h( PM_Num::qty( $area ) ); ?> см²)
				· свободно ≈ <b><?php echo pm_h( PM_Num::plain( floor( $avail / $area * 10 ) / 10, 1 ) ); ?></b> листа</div>
		<?php endif; ?>
		<div>На складе: <b class="total"><?php echo pm_h( PM_Num::qty( $m->stock ) . ' ' . $unit['label'] ); ?></b>
			· в резерве под заказы: <b><?php echo pm_h( PM_Num::qty( $m->reserved ) . ' ' . $unit['label'] ); ?></b>
			· свободно: <b><?php echo pm_h( PM_Num::qty( $avail ) . ' ' . $unit['label'] ); ?></b></div>

		<div class="row" style="margin-top:14px">
			<?php echo PM_View::form_open( 'material_receive', array( 'id' => $m->id ), 'class="row" style="flex:1 1 320px"' ); // phpcs:ignore WordPress.Security.EscapeOutput ?>
				<div><label>Приход</label><input type="number" name="quantity" step="<?php echo pm_a( $step ); ?>" min="0" required></div>
				<div><label>Комментарий</label><input type="text" name="note" maxlength="255" placeholder="Поставка, накладная…"></div>
				<div class="fixed"><button type="submit">Принять</button></div>
			</form>
			<?php if ( $area !== null ) : ?>
			<?php echo PM_View::form_open( 'material_receive_sheets', array( 'id' => $m->id ), 'class="row" style="flex:1 1 320px"' ); // phpcs:ignore WordPress.Security.EscapeOutput ?>
				<div><label><?php echo $is_foil ? 'Приход рулонами' : 'Приход листами'; ?></label><input type="number" name="sheets" step="1" min="1" required></div>
				<div><label>Комментарий</label><input type="text" name="note" maxlength="255"></div>
				<div class="fixed"><button type="submit"><?php echo $is_foil ? 'Принять рулоны' : 'Принять листы'; ?></button></div>
			</form>
			<?php endif; ?>
			<?php echo PM_View::form_open( 'material_consume', array( 'id' => $m->id ), 'class="row" style="flex:1 1 320px"' ); // phpcs:ignore WordPress.Security.EscapeOutput ?>
				<div><label>Расход</label><input type="number" name="quantity" step="<?php echo pm_a( $step ); ?>" min="0" required></div>
				<div><label>Комментарий</label><input type="text" name="note" maxlength="255" placeholder="Заказ №…"></div>
				<div class="fixed"><button type="submit" class="secondary">Списать</button></div>
			</form>
		</div>
	</section>

	<section>
		<h2>История движения</h2>
		<?php if ( ! $moves ) : ?>
			<div class="muted">Записей нет</div>
		<?php else : ?>
			<div class="table-wrap"><table>
				<thead><tr><th>Дата</th><th>Операция</th><th class="num">Количество</th><th>Комментарий</th></tr></thead>
				<tbody>
				<?php foreach ( $moves as $mv ) : ?>
					<tr>
						<td><?php echo pm_h( pm_date( $mv->created_at, true ) ); ?></td>
						<td><?php echo $mv->type === 'IN' ? 'Приход' : 'Расход'; ?></td>
						<td class="num"><?php echo pm_h( ( $mv->type === 'IN' ? '+' : '−' ) . PM_Num::qty( $mv->quantity ) ); ?></td>
						<td class="muted"><?php echo pm_h( $mv->note ); ?></td>
					</tr>
				<?php endforeach; ?>
				</tbody>
			</table></div>
		<?php endif; ?>
	</section>

	<section>
		<details>
			<summary>Изменить материал</summary>
			<?php echo PM_View::form_open( 'material_update', array( 'id' => $m->id ), 'class="row"' ); // phpcs:ignore WordPress.Security.EscapeOutput ?>
				<div><label>Название</label><input type="text" name="name" value="<?php echo pm_a( $m->name ); ?>" required></div>
				<?php if ( $m->category !== 'PAPER' ) : ?>
					<div><label><?php echo $is_foil ? 'Ширина рулона, ' : 'Ширина листа, '; ?><?php echo pm_h( $ul ); ?></label>
						<input type="number" name="width" step="any" min="0" value="<?php echo pm_a( PM_Units::fmt_len( $m->sheet_width_cm, $lu ) ); ?>"></div>
				<?php endif; ?>
				<?php if ( $is_plate ) : ?>
					<div><label>Длина листа, <?php echo pm_h( $ul ); ?></label>
						<input type="number" name="length" step="any" min="0" value="<?php echo pm_a( PM_Units::fmt_len( $m->sheet_length_cm, $lu ) ); ?>"></div>
				<?php endif; ?>
				<?php if ( $is_foil ) : ?>
					<div><label>Намотка, м</label>
						<input type="number" name="winding" step="any" min="0" value="<?php echo pm_a( $m->sheet_length_cm !== null ? PM_Num::plain( PM_Materials::roll_length_m( $m ) ) : '' ); ?>"></div>
				<?php endif; ?>
				<?php if ( $m->category === 'PAPER' ) : ?>
					<div><label>Размер листа</label><input type="text" name="sheet_size" value="<?php echo pm_a( $m->sheet_size ); ?>" maxlength="50"></div>
				<?php endif; ?>
				<div><label>Примечание</label><input type="text" name="note" value="<?php echo pm_a( $m->note ); ?>" maxlength="255"></div>
				<div class="fixed"><button type="submit">Сохранить</button></div>
			</form>
		</details>
	</section>
	<section>
		<?php echo PM_View::form_open( 'material_delete', array( 'id' => $m->id ), 'data-confirm="Удалить материал вместе с историей движения?"' ); // phpcs:ignore WordPress.Security.EscapeOutput ?>
			<button type="submit" class="danger">Удалить материал</button>
			<span class="muted"> — если он внесён по ошибке. Материал, который уже есть в заказах, удалить нельзя.</span>
		</form>
	</section>
</main>
	<?php
	return;
}

// ---------- список по разделам ----------
?>
<main class="p-materials">
	<h1>Склад материалов</h1>

	<div class="toolbar" style="margin-bottom:18px">
		<span class="muted">Остатки по разделам склада</span>
		<button type="button" data-pm-open="newMaterial">+ Новый материал</button>
	</div>

	<?php foreach ( PM_Units::CATEGORIES as $code => $cat ) : ?>
		<?php $list = PM_Materials::by_category( $code ); ?>
		<section>
			<h2><?php echo pm_h( $cat['label'] ); ?></h2>
			<?php if ( ! $list ) : ?>
				<div class="muted">Материалов нет</div>
			<?php else : ?>
				<div class="table-wrap"><table>
					<thead><tr><th>Материал</th><th><?php echo $code === 'FOIL' ? 'Рулон' : 'Размер листа'; ?></th><th class="num">На складе</th><th class="num">В резерве</th><th class="num">Свободно</th><th>Примечание</th><th></th></tr></thead>
					<tbody>
					<?php foreach ( $list as $m ) : ?>
						<?php
						$unit = PM_Materials::unit( $m );
						$area = PM_Materials::sheet_area( $m );
						if ( $code === 'PAPER' ) {
							$size = (string) $m->sheet_size;
						} elseif ( $code === 'FOIL' ) {
							$size = $area !== null ? 'рулон ' . PM_Units::fmt_len( $m->sheet_width_cm, $lu ) . ' ' . $ul . ' × ' . PM_Num::plain( PM_Materials::roll_length_m( $m ) ) . ' м' : '—';
						} else {
							$size = $area !== null ? PM_Units::fmt_len( $m->sheet_width_cm, $lu ) . ' × ' . PM_Units::fmt_len( $m->sheet_length_cm, $lu ) . ' ' . $ul . ' (' . PM_Num::plain( $area ) . ' см²)' : '—';
						}
						?>
						<tr>
							<td><a class="olink order-id" href="<?php echo esc_url( PM_View::url( 'pm-materials', array( 'id' => $m->id ) ) ); ?>"><?php echo pm_h( $m->name ); ?></a></td>
							<td><?php echo pm_h( $size ); ?></td>
							<td class="num"><?php echo pm_h( PM_Num::qty( $m->stock ) . ' ' . $unit['label'] ); ?></td>
							<td class="num muted"><?php echo (float) $m->reserved > 0 ? pm_h( PM_Num::qty( $m->reserved ) . ' ' . $unit['label'] ) : '—'; ?></td>
							<td class="num total"><?php echo pm_h( PM_Num::qty( PM_Materials::available( $m ) ) . ' ' . $unit['label'] ); ?></td>
							<td class="muted"><?php echo pm_h( $m->note ); ?></td>
							<td class="num">
								<?php echo PM_View::form_open( 'material_delete', array( 'id' => $m->id ), 'class="inline" data-confirm="Удалить материал вместе с историей движения?"' ); // phpcs:ignore WordPress.Security.EscapeOutput ?>
									<button type="submit" class="danger small" title="Удалить материал">Удалить</button>
								</form>
							</td>
						</tr>
					<?php endforeach; ?>
					</tbody>
				</table></div>
			<?php endif; ?>
		</section>
	<?php endforeach; ?>

	<dialog id="newMaterial" data-open-on-error>
		<div class="dlg-head"><h2>Новый материал</h2></div>
		<div class="dlg-body">
			<?php echo PM_View::form_open( 'material_create', array(), 'id="newMaterialForm" class="row"' ); // phpcs:ignore WordPress.Security.EscapeOutput ?>
				<div><label>Раздел</label>
					<select name="category" id="category" data-toggle-groups>
						<?php foreach ( PM_Units::CATEGORIES as $code => $cat ) : ?>
							<option value="<?php echo pm_a( $code ); ?>"><?php echo pm_h( $cat['label'] . ' (' . PM_Units::STOCK_UNITS[ $cat['unit'] ]['label'] . ')' ); ?></option>
						<?php endforeach; ?>
					</select></div>
				<div data-for="PAPER MAGNESIUM BRASS"><label>Название</label><input type="text" name="name" required></div>

				<div data-for="FOIL"><label>Тип фольги</label>
					<select name="foil_type" required>
						<?php foreach ( PM_Units::FOIL_TYPES as $code => $label ) : ?>
							<option value="<?php echo pm_a( $code ); ?>"><?php echo pm_h( $label ); ?></option>
						<?php endforeach; ?>
					</select></div>
				<div data-for="FOIL"><label>Ширина рулона, <?php echo pm_h( $ul ); ?></label>
					<input type="number" name="width" step="any" min="0" required placeholder="62"></div>
				<div data-for="FOIL"><label>Намотка, м</label>
					<input type="number" name="winding" step="any" min="0" required placeholder="122"></div>
				<div data-for="FOIL"><label>Рулонов</label><input type="number" name="rolls" step="1" min="0" value="1"></div>

				<div data-for="PAPER"><label>Размер листа</label><input type="text" name="sheet_size" maxlength="50" placeholder="например 70x100 см"></div>
				<div data-for="PAPER"><label>Начальный остаток, листов</label><input type="number" name="stock" step="1" min="0" value="0"></div>

				<div data-for="MAGNESIUM BRASS"><label>Ширина листа, <?php echo pm_h( $ul ); ?></label><input type="number" name="width" step="any" min="0" required></div>
				<div data-for="MAGNESIUM BRASS"><label>Длина листа, <?php echo pm_h( $ul ); ?></label><input type="number" name="length" step="any" min="0" required></div>
				<div data-for="MAGNESIUM BRASS"><label>Начальный остаток, листов</label><input type="number" name="sheets" step="1" min="0" value="0"></div>

				<div><label>Примечание</label><input type="text" name="note" maxlength="255"></div>
			</form>
			<p class="hint" id="foilHint" hidden>Остаток фольги считается в см²: ширина рулона × намотка × число рулонов. Например, 62 см × 122 м даёт 62 × 12 200 = 756 400 см² на рулон.</p>
			<div class="dlg-actions">
				<button type="button" class="secondary" data-pm-close>Отмена</button>
				<button type="submit" form="newMaterialForm">Добавить материал</button>
			</div>
		</div>
	</dialog>
</main>
