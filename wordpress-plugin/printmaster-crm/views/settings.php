<?php
if ( ! defined( 'ABSPATH' ) ) {
	exit;
}
$s = PM_Settings::get();
$f = function ( $key ) use ( $s ) {
	return pm_a( PM_Num::plain( $s[ $key ], 4 ) );
};
?>
<main class="p-settings">
	<h1>Настройки цен и единиц измерения</h1>

	<?php echo PM_View::form_open( 'settings_save' ); // phpcs:ignore WordPress.Security.EscapeOutput ?>
		<section>
			<h2>Единицы измерения</h2>
			<div class="row">
				<div>
					<label for="length_unit">Размеры в расчётах</label>
					<select id="length_unit" name="length_unit">
						<?php foreach ( PM_Units::LENGTH as $code => $u ) : ?>
							<option value="<?php echo pm_a( $code ); ?>" <?php selected( $s['length_unit'], $code ); ?>><?php echo pm_h( $u['label'] ); ?></option>
						<?php endforeach; ?>
					</select>
					<div class="hint">Ширина и длина вводятся в этих единицах. Уже сохранённые расчёты пересчитываются при показе.</div>
				</div>
			</div>
		</section>

		<section>
			<h2>Тиснение</h2>
			<div class="row">
				<div><label for="montage_price">Приладка</label>
					<input type="number" id="montage_price" name="montage_price" value="<?php echo $f( 'montage_price' ); // phpcs:ignore WordPress.Security.EscapeOutput ?>" step="any" min="0" required></div>
				<div><label for="one_ottisk_price">Цена одного оттиска</label>
					<input type="number" id="one_ottisk_price" name="one_ottisk_price" value="<?php echo $f( 'one_ottisk_price' ); // phpcs:ignore WordPress.Security.EscapeOutput ?>" step="any" min="0" required></div>
				<div><label for="foil_price">Цена фольги</label>
					<input type="number" id="foil_price" name="foil_price" value="<?php echo $f( 'foil_price' ); // phpcs:ignore WordPress.Security.EscapeOutput ?>" step="any" min="0" required></div>
				<div><label for="foil_area_unit">за единицу площади</label>
					<select id="foil_area_unit" name="foil_area_unit">
						<?php foreach ( PM_Units::AREA as $code => $u ) : ?>
							<option value="<?php echo pm_a( $code ); ?>" <?php selected( $s['foil_area_unit'], $code ); ?>><?php echo pm_h( $u['label'] ); ?></option>
						<?php endforeach; ?>
					</select></div>
			</div>
		</section>

		<section>
			<h2>Печать</h2>
			<div class="row">
				<div><label for="print_price">Цена за один лист, одна сторона, бумага 90 г</label>
					<input type="number" id="print_price" name="print_price" value="<?php echo $f( 'print_price' ); // phpcs:ignore WordPress.Security.EscapeOutput ?>" step="any" min="0" required></div>
			</div>
		</section>

		<section>
			<h2>Клише</h2>
			<div class="row">
				<div><label for="cliche_price">Цена клише</label>
					<input type="number" id="cliche_price" name="cliche_price" value="<?php echo $f( 'cliche_price' ); // phpcs:ignore WordPress.Security.EscapeOutput ?>" step="any" min="0" required></div>
				<div><label for="cliche_area_unit">за единицу площади</label>
					<select id="cliche_area_unit" name="cliche_area_unit">
						<?php foreach ( PM_Units::AREA as $code => $u ) : ?>
							<option value="<?php echo pm_a( $code ); ?>" <?php selected( $s['cliche_area_unit'], $code ); ?>><?php echo pm_h( $u['label'] ); ?></option>
						<?php endforeach; ?>
					</select></div>
				<div><label for="cliche_min_price">Минимальная цена одного клише</label>
					<input type="number" id="cliche_min_price" name="cliche_min_price" value="<?php echo $f( 'cliche_min_price' ); // phpcs:ignore WordPress.Security.EscapeOutput ?>" step="any" min="0" required></div>
			</div>
		</section>

		<section>
			<h2>Коэффициент</h2>
			<div class="row">
				<div><label for="markup">Коэффициент для тиснения</label>
					<input type="number" id="markup" name="markup" value="<?php echo $f( 'markup' ); // phpcs:ignore WordPress.Security.EscapeOutput ?>" step="any" min="0.0001" required>
					<div class="hint">1.25 означает +25% к стоимости тиснения. На печать и клише не действует.</div></div>
			</div>
		</section>

		<button type="submit">Сохранить</button>
	</form>
</main>
