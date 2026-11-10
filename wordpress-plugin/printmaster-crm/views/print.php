<?php
if ( ! defined( 'ABSPATH' ) ) {
	exit;
}
/** Печатная карточка заказа: половина A4 (A5), которую можно сложить в конверт. Переменные: $order, $customer, $items. */
$addr = PM_Customers::address_line( $customer );
?><!DOCTYPE html>
<html lang="ru">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="robots" content="noindex,nofollow">
<title>Заказ №<?php echo (int) $order->id; ?> — карточка</title>
<style>
        /* Лист A4: верхняя половина (A5, 210×148,5 мм) — карточка, нижняя — оборот конверта.
           Лист складывается пополам по линии сгиба, склеивается по бокам. */
        @page { size: A4 portrait; margin: 0; }
        * { box-sizing: border-box; }
        html, body { margin: 0; padding: 0; background: #e9ecf1; color: #000;
            font: 10pt/1.35 "Helvetica Neue", Arial, "Segoe UI", sans-serif; }
        .toolbar { max-width: 210mm; margin: 12px auto; display: flex; gap: 12px; align-items: center;
            justify-content: space-between; font-family: Arial, sans-serif; font-size: 14px; color: #3d4752; }
        .toolbar button, .toolbar a { font: inherit; padding: 8px 16px; border-radius: 8px; border: 0;
            background: #4a8120; color: #fff; text-decoration: none; cursor: pointer; }
        .toolbar a.secondary { background: #d9dee5; color: #1f2933; }
        .sheet { width: 210mm; height: 297mm; margin: 0 auto 24px; background: #fff; position: relative;
            box-shadow: 0 4px 20px rgba(0,0,0,.18); overflow: hidden; }
        .card { position: absolute; top: 0; left: 0; width: 210mm; height: 148.5mm; padding: 10mm 12mm 7mm;
            display: flex; flex-direction: column; gap: 3mm; }
        .fold { position: absolute; top: 148.5mm; left: 0; right: 0; border-top: 0.4mm dashed #888; }
        .fold span { position: absolute; top: -3.1mm; left: 50%; transform: translateX(-50%); background: #fff;
            padding: 0 3mm; font-size: 7pt; color: #777; letter-spacing: .5px; }
        .back { position: absolute; top: 148.5mm; left: 0; width: 210mm; height: 148.5mm; }
        .back .glue { position: absolute; top: 0; bottom: 0; width: 9mm; border: 0 dashed #aaa; color: #aaa; font-size: 7pt;
            display: flex; align-items: center; justify-content: center; }
        .back .glue.l { left: 0; border-right-width: 0.3mm; }
        .back .glue.r { right: 0; border-left-width: 0.3mm; }
        .back .glue span { writing-mode: vertical-rl; letter-spacing: 1px; }
        .back .note { position: absolute; left: 0; right: 0; bottom: 6mm; text-align: center; font-size: 7pt; color: #aaa; }

        .head { display: flex; justify-content: space-between; align-items: flex-start; border-bottom: 0.6mm solid #000; padding-bottom: 2mm; }
        .head .num { font-size: 24pt; font-weight: 800; line-height: 1; }
        .head .num small { font-size: 9pt; font-weight: 500; margin-left: 4mm; }
        .head .brand { font-size: 8pt; color: #555; text-align: right; }
        .client { display: grid; grid-template-columns: 1fr auto; gap: 1mm 8mm; }
        .client .name { font-size: 15pt; font-weight: 700; grid-column: 1 / -1; }
        .lbl { font-size: 7pt; text-transform: uppercase; letter-spacing: .6px; color: #555; }
        .desc { font-size: 11pt; font-weight: 600; }
        .works { white-space: pre-line; font-size: 9.5pt; }
        .mats { font-size: 9pt; }
        .checks { display: flex; flex-wrap: wrap; gap: 1.5mm 6mm; font-size: 9.5pt; padding-top: 1mm; }
        .checks .box { display: inline-block; width: 4.2mm; height: 4.2mm; border: 0.4mm solid #000; margin-right: 1.8mm; vertical-align: -0.9mm; }
        .notes { flex: 1; min-height: 14mm; border-top: 0.3mm solid #999; position: relative; overflow: hidden;
            background-image: repeating-linear-gradient(to bottom, transparent 0, transparent 6.2mm, #bbb 6.2mm, #bbb 6.5mm); }
        .notes .lbl { position: absolute; top: 0.6mm; left: 0; background: #fff; padding-right: 2mm; }
        .foot { display: flex; justify-content: space-between; font-size: 8.5pt; align-items: flex-end; gap: 8mm; }
        .foot .line { flex: 1; border-bottom: 0.3mm solid #000; height: 5mm; }
        .foot .item { display: flex; align-items: flex-end; gap: 2mm; flex: 1; }

        @media print {
            html, body { background: #fff; }
            .toolbar { display: none; }
            .sheet { margin: 0; box-shadow: none; page-break-after: avoid; }
        }
    </style>
</head>
<body>
<div class="toolbar">
	<span>Печать на листе A4: карточка займёт верхнюю половину. Сложите лист по линии сгиба и склейте по бокам — получится конверт A5.</span>
	<span style="display:flex;gap:8px">
		<a class="secondary" href="<?php echo esc_url( PM_View::url( 'pm-orders', array( 'order' => $order->id ) ) ); ?>">← К заказу</a>
		<button type="button" onclick="window.print()">Печать</button>
	</span>
</div>

<div class="sheet">
	<div class="card">
		<div class="head">
			<div class="num">№ <span><?php echo (int) $order->id; ?></span>
				<small>от <?php echo esc_html( pm_date( $order->created_at ) ); ?></small></div>
			<div class="brand">PrintMaster<br>
				<span><?php echo esc_html( PM_Units::status_label( $order->status ) ); ?></span></div>
		</div>

		<div class="client">
			<div class="name"><?php echo esc_html( $customer->name ); ?></div>
			<div><span class="lbl">Телефон</span> <b><?php echo esc_html( $customer->phone ); ?></b>
				&nbsp;&nbsp; <span class="lbl">Email</span> <b><?php echo esc_html( $customer->email ); ?></b></div>
			<?php if ( $addr !== '' ) : ?>
				<div><span class="lbl">Адрес</span> <span><?php echo esc_html( $addr ); ?></span></div>
			<?php endif; ?>
		</div>

		<div>
			<div class="lbl">Описание</div>
			<div class="desc"><?php echo esc_html( $order->product ); ?></div>
		</div>

		<?php if ( $order->details !== null && $order->details !== '' ) : ?>
			<div>
				<div class="lbl">Работы</div>
				<div class="works"><?php echo esc_html( $order->details ); ?></div>
			</div>
		<?php endif; ?>

		<?php if ( $items ) : ?>
			<div class="mats">
				<span class="lbl">Материалы</span>
				<?php
				$parts = array();
				foreach ( $items as $u ) {
					$parts[] = $u->material_name . ' — ' . PM_Num::qty( $u->quantity ) . ' ' . PM_Units::cat_unit( $u->material_category )['label'];
				}
				echo esc_html( implode( '; ', $parts ) );
				?>
			</div>
		<?php endif; ?>

		<div class="checks">
			<span><i class="box"></i>Макет получен</span>
			<?php if ( (float) $order->cliche_price > 0 ) : ?><span><i class="box"></i>Клише</span><?php endif; ?>
			<?php if ( (float) $order->printing_price > 0 ) : ?><span><i class="box"></i>Печать</span><?php endif; ?>
			<?php if ( (float) $order->postpress_price > 0 ) : ?><span><i class="box"></i>Тиснение</span><?php endif; ?>
			<span><i class="box"></i>Готов</span>
			<span><i class="box"></i>Оплачен</span>
			<span><i class="box"></i>Выдан клиенту</span>
		</div>

		<div class="notes"><span class="lbl">Заметки</span></div>

		<div class="foot">
			<div class="item"><span>Срок:</span><span class="line"></span></div>
			<div class="item"><span>Принял:</span><span class="line"></span></div>
			<div><span class="lbl">Сумма</span>
				<b><?php echo esc_html( PM_Num::money( $order->total_cost ) ); ?></b></div>
		</div>
	</div>

	<div class="fold"><span>СГИБ · сложить пополам</span></div>
	<div class="back">
		<div class="glue l"><span>КЛЕИТЬ</span></div>
		<div class="glue r"><span>КЛЕИТЬ</span></div>
		<div class="note">оборот конверта: вложения (распечатка макета и т. п.)</div>
	</div>
</div>
</body>
</html>
