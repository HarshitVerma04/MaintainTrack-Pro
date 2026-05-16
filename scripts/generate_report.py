"""
generate_report.py -- MaintainTrack Pro
---------------------------------------
Generates a PDF maintenance report for a given equipment ID.

Usage (called by Java ReportService via ProcessBuilder):
    python scripts/generate_report.py --equipment-id 1 --output reports/report_1.pdf
    python scripts/generate_report.py --all --output reports/fleet_report.pdf

Dependencies: fpdf2, sqlite3 (stdlib)
"""

import argparse
import sqlite3
import os
import sys
from datetime import date, datetime
from fpdf import FPDF
from fpdf.enums import XPos, YPos

DB_PATH = os.path.join(os.path.dirname(__file__), '..', 'data', 'maintaintrack.db')


def safe(text):
    """
    Sanitize any string for fpdf Latin-1 fonts.
    Replaces common Unicode punctuation with ASCII equivalents
    then drops anything still outside Latin-1 range.
    Called on every value before it touches a pdf.cell().
    """
    if text is None:
        return '-'
    text = str(text)
    replacements = {
        '\u2014': '--',
        '\u2013': '-',
        '\u2018': "'",
        '\u2019': "'",
        '\u201c': '"',
        '\u201d': '"',
        '\u20b9': 'Rs',
        '\u2022': '*',
        '\u00d7': 'x',
    }
    for uc, asc in replacements.items():
        text = text.replace(uc, asc)
    return text.encode('latin-1', errors='ignore').decode('latin-1')

NAVY  = (27,  58,  107)
BLUE  = (46,  134, 222)
LIGHT = (214, 232, 250)
GREEN = (26,  122, 74)
RED   = (139, 26,  26)
AMBER = (184, 92,  0)
GRAY  = (92,  107, 138)
WHITE = (255, 255, 255)
LGRAY = (244, 246, 250)


def get_conn():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn


def fetch_equipment(conn, equipment_id):
    return conn.execute(
        "SELECT * FROM EQUIPMENT WHERE id=?;", (equipment_id,)
    ).fetchone()


def fetch_all_equipment(conn):
    return conn.execute(
        "SELECT * FROM EQUIPMENT ORDER BY name;"
    ).fetchall()


def fetch_maintenance_logs(conn, equipment_id):
    return conn.execute("""
        SELECT done_on, done_by, notes FROM MAINTENANCE_LOG
        WHERE equipment_id=? ORDER BY done_on DESC;
    """, (equipment_id,)).fetchall()


def fetch_breakdown_logs(conn, equipment_id):
    return conn.execute("""
        SELECT occurred_on, description, resolved_by FROM BREAKDOWN_LOG
        WHERE equipment_id=? ORDER BY occurred_on DESC;
    """, (equipment_id,)).fetchall()


def fetch_issue_records(conn, equipment_id):
    return conn.execute("""
        SELECT ir.issued_on, p.name AS part_name, ir.qty, p.unit,
               ir.issued_by, ir.type,
               ROUND(ir.qty * p.unit_cost, 2) AS line_cost
        FROM ISSUE_RECORD ir JOIN PART p ON ir.part_id=p.id
        WHERE ir.equipment_id=? ORDER BY ir.issued_on DESC;
    """, (equipment_id,)).fetchall()


class MaintainTrackPDF(FPDF):

    def header(self):
        self.set_fill_color(*NAVY)
        self.rect(0, 0, 210, 18, 'F')
        self.set_text_color(*WHITE)
        self.set_font('Helvetica', 'B', 13)
        self.set_xy(10, 4)
        # FIX 1: replaced em dash with plain hyphen, updated deprecated ln param
        self.cell(0, 10, 'MaintainTrack Pro - Equipment Report',
                  new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.set_font('Helvetica', '', 9)
        self.set_xy(0, 4)
        self.cell(200, 10,
                  f'Generated: {datetime.now().strftime("%d %b %Y  %H:%M")}',
                  align='R',
                  new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.ln(8)

    def footer(self):
        self.set_y(-12)
        self.set_font('Helvetica', 'I', 8)
        self.set_text_color(*GRAY)
        self.cell(0, 6,
                  f'MaintainTrack Pro  |  Page {self.page_no()}  |  Confidential',
                  align='C',
                  new_x=XPos.LMARGIN, new_y=YPos.NEXT)

    def section_title(self, title):
        self.set_fill_color(*LIGHT)
        self.set_text_color(*NAVY)
        self.set_font('Helvetica', 'B', 11)
        # FIX 2: ln=True -> new_x/new_y
        self.cell(0, 8, safe(f'  {title}'),
                  fill=True,
                  new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.ln(2)

    def kv_row(self, label, value, label_w=55):
        self.set_font('Helvetica', 'B', 9)
        self.set_text_color(*GRAY)
        # FIX 3: ln=False -> new_x=XPos.RIGHT, new_y=YPos.TOP
        self.cell(label_w, 7, label,
                  new_x=XPos.RIGHT, new_y=YPos.TOP)
        self.set_font('Helvetica', '', 9)
        self.set_text_color(30, 30, 30)
        self.cell(0, 7, safe(value),
                  new_x=XPos.LMARGIN, new_y=YPos.NEXT)

    def table_header(self, cols):
        self.set_fill_color(*NAVY)
        self.set_text_color(*WHITE)
        self.set_font('Helvetica', 'B', 8)
        for i, (label, w) in enumerate(cols):
            last = (i == len(cols) - 1)
            self.cell(w, 7, label,
                      border=0, fill=True,
                      new_x=XPos.LMARGIN if last else XPos.RIGHT,
                      new_y=YPos.NEXT    if last else YPos.TOP)

    def table_row(self, values, cols, i):
        self.set_fill_color(*(LGRAY if i % 2 == 0 else WHITE))
        self.set_text_color(30, 30, 30)
        self.set_font('Helvetica', '', 8)
        for j, ((_, w), val) in enumerate(zip(cols, values)):
            last = (j == len(cols) - 1)
            # FIX 4: replace None/-/em-dash with plain dash
            display = safe(val)
            self.cell(w, 6, display,
                      border=0, fill=True,
                      new_x=XPos.LMARGIN if last else XPos.RIGHT,
                      new_y=YPos.NEXT    if last else YPos.TOP)

    def summary_box(self, label, value, color=BLUE):
        x, y = self.get_x(), self.get_y()
        self.set_fill_color(*color)
        self.rect(x, y, 58, 18, 'F')
        self.set_text_color(*WHITE)
        self.set_font('Helvetica', '', 8)
        self.set_xy(x + 2, y + 2)
        self.cell(54, 6, label,
                  new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.set_font('Helvetica', 'B', 14)
        self.set_xy(x + 2, y + 8)
        self.cell(54, 8, str(value),
                  new_x=XPos.RIGHT, new_y=YPos.TOP)
        self.set_xy(x + 62, y)


def build_equipment_report(equipment_id, output_path):
    conn = get_conn()
    eq   = fetch_equipment(conn, equipment_id)
    if not eq:
        print(f"[ERROR] Equipment ID {equipment_id} not found.")
        conn.close()
        return False

    maintenance = fetch_maintenance_logs(conn, equipment_id)
    breakdowns  = fetch_breakdown_logs(conn, equipment_id)
    issues      = fetch_issue_records(conn, equipment_id)
    total_cost  = sum(r['line_cost'] for r in issues if r['type'] == 'issue')
    conn.close()

    status_color = {
        'Operational':       GREEN,
        'Under Maintenance': AMBER,
        'Out of Service':    RED
    }.get(eq['status'], GRAY)

    pdf = MaintainTrackPDF()
    pdf.set_auto_page_break(auto=True, margin=15)
    pdf.add_page()

    # ── Equipment Profile ─────────────────────────────────────────────────
    pdf.section_title('Equipment Profile')
    pdf.kv_row('Name:', safe(eq['name']))
    pdf.kv_row('Location:', safe(eq['location']))
    pdf.kv_row('Interval (days):', str(eq['interval_days']))
    pdf.kv_row('Next Due:', safe(eq['next_maintenance_date'] or '-'))

    pdf.set_font('Helvetica', 'B', 9)
    pdf.set_text_color(*GRAY)
    pdf.cell(55, 7, 'Status:',
             new_x=XPos.RIGHT, new_y=YPos.TOP)
    pdf.set_fill_color(*status_color)
    pdf.set_text_color(*WHITE)
    pdf.cell(50, 6, safe(f'  {eq["status"]}'), fill=True,
             new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    pdf.ln(10)

    # ── Summary boxes ─────────────────────────────────────────────────────
    pdf.summary_box('Maintenance Jobs', len(maintenance), BLUE)
    pdf.summary_box('Breakdowns', len(breakdowns),
                    RED if len(breakdowns) > 0 else GREEN)
    pdf.summary_box('Parts Cost', f'Rs {total_cost:,.0f}', NAVY)
    pdf.ln(26)

    # ── Maintenance log ───────────────────────────────────────────────────
    pdf.section_title(f'Maintenance History  ({len(maintenance)} jobs)')
    if maintenance:
        cols = [('Date', 28), ('Done By', 32), ('Notes', 130)]
        pdf.table_header(cols)
        for i, row in enumerate(maintenance):
            pdf.table_row(
                [safe(row['done_on']),
                 safe(row['done_by']),
                 safe(row['notes'])],
                cols, i
            )
    else:
        pdf.set_font('Helvetica', 'I', 9)
        pdf.set_text_color(*GRAY)
        pdf.cell(0, 7, '  No maintenance jobs recorded.',
                 new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    pdf.ln(6)

    # ── Breakdown log ─────────────────────────────────────────────────────
    pdf.section_title(f'Breakdown History  ({len(breakdowns)} incidents)')
    if breakdowns:
        cols = [('Date', 28), ('Resolved By', 32), ('Description', 130)]
        pdf.table_header(cols)
        for i, row in enumerate(breakdowns):
            pdf.table_row(
                [safe(row['occurred_on']),
                 safe(row['resolved_by'] or 'Unresolved'),
                 safe(row['description'])],
                cols, i
            )
    else:
        pdf.set_font('Helvetica', 'I', 9)
        pdf.set_text_color(*GRAY)
        pdf.cell(0, 7, '  No breakdown incidents recorded.',
                 new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    pdf.ln(6)

    # ── Parts issued ──────────────────────────────────────────────────────
    issued = [r for r in issues if r['type'] == 'issue']
    pdf.section_title(f'Parts Issued  ({len(issued)} transactions)')
    if issues:
        cols = [('Date', 26), ('Part', 58), ('Qty', 14), ('Unit', 14),
                ('Type', 22), ('By', 28), ('Cost (Rs)', 28)]
        pdf.table_header(cols)
        for i, row in enumerate(issues):
            cost_str = (f'{row["line_cost"]:,.2f}'
                        if row['type'] == 'issue' else '-')
            pdf.table_row(
                [safe(row['issued_on']), safe(row['part_name']), str(row['qty']),
                 safe(row['unit']), safe(row['type'].capitalize()),
                 safe(row['issued_by']), safe(cost_str)],
                cols, i
            )
        pdf.set_font('Helvetica', 'B', 9)
        pdf.set_text_color(*NAVY)
        pdf.cell(0, 8,
                 f'  Total Parts Cost:  Rs {total_cost:,.2f}',
                 align='R',
                 new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    else:
        pdf.set_font('Helvetica', 'I', 9)
        pdf.set_text_color(*GRAY)
        pdf.cell(0, 7, '  No parts issued.',
                 new_x=XPos.LMARGIN, new_y=YPos.NEXT)

    os.makedirs(os.path.dirname(os.path.abspath(output_path)), exist_ok=True)
    pdf.output(output_path)
    print(f"[OK] Report saved: {output_path}")
    return True


def build_fleet_report(output_path):
    conn      = get_conn()
    equipment = fetch_all_equipment(conn)

    pdf = MaintainTrackPDF()
    pdf.set_auto_page_break(auto=True, margin=15)
    pdf.add_page()

    pdf.section_title('Fleet Summary Report')
    pdf.set_font('Helvetica', '', 9)
    pdf.set_text_color(*GRAY)
    pdf.cell(0, 6,
             f'  Total equipment: {len(equipment)}   |   '
             f'Report date: {date.today()}',
             new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    pdf.ln(4)

    cols = [('ID', 12), ('Name', 50), ('Location', 36),
            ('Status', 36), ('Next Due', 28), ('Jobs', 16), ('BDs', 12)]
    pdf.table_header(cols)

    for i, eq in enumerate(equipment):
        jobs = conn.execute(
            "SELECT COUNT(*) FROM MAINTENANCE_LOG WHERE equipment_id=?;",
            (eq['id'],)).fetchone()[0]
        bds = conn.execute(
            "SELECT COUNT(*) FROM BREAKDOWN_LOG WHERE equipment_id=?;",
            (eq['id'],)).fetchone()[0]
        pdf.table_row(
            [str(eq['id']), safe(eq['name']), safe(eq['location']),
             safe(eq['status']), safe(eq['next_maintenance_date'] or '-'),
             str(jobs), str(bds)],
            cols, i
        )

    conn.close()
    os.makedirs(os.path.dirname(os.path.abspath(output_path)), exist_ok=True)
    pdf.output(output_path)
    print(f"[OK] Fleet report saved: {output_path}")
    return True


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--equipment-id', type=int)
    parser.add_argument('--all', action='store_true')
    parser.add_argument('--output', required=True)
    args = parser.parse_args()

    if not os.path.exists(DB_PATH):
        print(f"[ERROR] DB not found: {DB_PATH}")
        sys.exit(1)

    if args.all:
        ok = build_fleet_report(args.output)
    elif args.equipment_id:
        ok = build_equipment_report(args.equipment_id, args.output)
    else:
        print("[ERROR] Provide --equipment-id <id> or --all")
        sys.exit(1)

    sys.exit(0 if ok else 1)
