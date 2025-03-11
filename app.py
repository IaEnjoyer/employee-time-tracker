from flask import Flask, render_template, request, redirect, url_for, flash, send_file
from flask_sqlalchemy import SQLAlchemy
from flask_login import LoginManager, UserMixin, login_user, login_required, logout_user, current_user
from flask_bcrypt import Bcrypt
from datetime import datetime, timedelta
import os
from reportlab.lib import colors
from reportlab.lib.pagesizes import letter
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import inch
from io import BytesIO
import locale

app = Flask(__name__)
app.config['SECRET_KEY'] = 'your-secret-key-here'
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///employee_time.db'
db = SQLAlchemy(app)
login_manager = LoginManager()
login_manager.init_app(app)
login_manager.login_view = 'login'
bcrypt = Bcrypt(app)

# Set locale for Spanish date formatting
locale.setlocale(locale.LC_TIME, 'es_ES.UTF-8')

class User(UserMixin, db.Model):
    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(80), unique=True, nullable=False)
    password = db.Column(db.String(120), nullable=False)
    name = db.Column(db.String(120), nullable=False)
    email = db.Column(db.String(120), unique=True, nullable=False)
    role = db.Column(db.String(20), nullable=False, default='employee')
    data_consent = db.Column(db.Boolean, nullable=False, default=False)
    consent_date = db.Column(db.DateTime, nullable=True)
    data_retention_days = db.Column(db.Integer, nullable=False, default=365)
    records = db.relationship('TimeRecord', backref='user', lazy=True)

class TimeRecord(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    user_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=False)
    check_in = db.Column(db.DateTime, nullable=False)
    check_out = db.Column(db.DateTime, nullable=True)

class DataProcessingConsent(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    user_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=False)
    consent_type = db.Column(db.String(50), nullable=False)
    granted = db.Column(db.Boolean, nullable=False)
    timestamp = db.Column(db.DateTime, nullable=False, default=datetime.utcnow)
    ip_address = db.Column(db.String(45), nullable=True)

@login_manager.user_loader
def load_user(user_id):
    return User.query.get(int(user_id))

def generate_pdf_report(records, user, report_type):
    buffer = BytesIO()
    doc = SimpleDocTemplate(buffer, pagesize=letter)
    elements = []
    styles = getSampleStyleSheet()
    
    # Create custom styles
    title_style = ParagraphStyle(
        'CustomTitle',
        parent=styles['Heading1'],
        fontSize=16,
        spaceAfter=30,
        alignment=1  # Center alignment
    )
    
    header_style = ParagraphStyle(
        'CustomHeader',
        parent=styles['Normal'],
        fontSize=12,
        spaceAfter=20
    )
    
    # Add company header
    elements.append(Paragraph("INFORME DE REGISTRO HORARIO", title_style))
    
    # Add report information
    period_text = {
        'week': 'Última Semana',
        'month': 'Último Mes',
        'year': 'Último Año'
    }
    
    elements.append(Paragraph(f"Empleado: {user.name}", header_style))
    elements.append(Paragraph(f"Periodo: {period_text[report_type]}", header_style))
    elements.append(Paragraph(f"Fecha de generación: {datetime.now().strftime('%d de %B de %Y')}", header_style))
    elements.append(Spacer(1, 20))
    
    # Prepare table data
    data = [['Fecha', 'Entrada', 'Salida', 'Horas Trabajadas']]
    total_hours = timedelta()
    
    for record in records:
        check_in_date = record.check_in.strftime('%d/%m/%Y')
        check_in_time = record.check_in.strftime('%H:%M')
        
        if record.check_out:
            check_out_time = record.check_out.strftime('%H:%M')
            duration = record.check_out - record.check_in
            hours_worked = f"{duration.seconds//3600}:{(duration.seconds//60)%60:02d}"
            total_hours += duration
        else:
            check_out_time = '-'
            hours_worked = '-'
            
        data.append([check_in_date, check_in_time, check_out_time, hours_worked])
    
    # Create table
    table = Table(data, colWidths=[2*inch, 1.5*inch, 1.5*inch, 2*inch])
    table.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), colors.grey),
        ('TEXTCOLOR', (0, 0), (-1, 0), colors.whitesmoke),
        ('ALIGN', (0, 0), (-1, -1), 'CENTER'),
        ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
        ('FONTSIZE', (0, 0), (-1, 0), 12),
        ('BOTTOMPADDING', (0, 0), (-1, 0), 12),
        ('BACKGROUND', (0, 1), (-1, -1), colors.white),
        ('TEXTCOLOR', (0, 1), (-1, -1), colors.black),
        ('FONTNAME', (0, 1), (-1, -1), 'Helvetica'),
        ('FONTSIZE', (0, 1), (-1, -1), 10),
        ('GRID', (0, 0), (-1, -1), 1, colors.black),
        ('ALIGN', (0, 0), (-1, -1), 'CENTER'),
        ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
    ]))
    
    elements.append(table)
    elements.append(Spacer(1, 20))
    
    # Add summary
    if total_hours.total_seconds() > 0:
        total_hours_str = f"{int(total_hours.total_seconds()//3600)}:{int((total_hours.total_seconds()//60)%60):02d}"
        elements.append(Paragraph(f"Total de horas trabajadas: {total_hours_str}", header_style))
    
    # Add footer
    elements.append(Spacer(1, 40))
    footer_text = """Este informe ha sido generado automáticamente por el sistema de control horario.
    Los datos mostrados están sujetos a la política de privacidad y protección de datos de la empresa."""
    elements.append(Paragraph(footer_text, styles['Normal']))
    
    # Build PDF
    doc.build(elements)
    buffer.seek(0)
    return buffer

@app.route('/generate_report', methods=['GET', 'POST'])
@login_required
def generate_report():
    if request.method == 'POST':
        report_type = request.form.get('report_type', 'week')
        
        # Calculate date range
        end_date = datetime.now()
        if report_type == 'week':
            start_date = end_date - timedelta(days=7)
        elif report_type == 'month':
            start_date = end_date - timedelta(days=30)
        else:  # year
            start_date = end_date - timedelta(days=365)
        
        # Get records
        records = TimeRecord.query.filter(
            TimeRecord.user_id == current_user.id,
            TimeRecord.check_in >= start_date,
            TimeRecord.check_in <= end_date
        ).order_by(TimeRecord.check_in.desc()).all()
        
        # Generate PDF
        pdf_buffer = generate_pdf_report(records, current_user, report_type)
        
        # Send file
        return send_file(
            pdf_buffer,
            download_name=f'registro_horario_{datetime.now().strftime("%Y%m%d")}.pdf',
            mimetype='application/pdf'
        )
    
    return render_template('report.html')

@app.route('/')
@login_required
def index():
    records = TimeRecord.query.filter_by(user_id=current_user.id).order_by(TimeRecord.check_in.desc()).all()
    active_record = TimeRecord.query.filter_by(user_id=current_user.id, check_out=None).first()
    return render_template('index.html', records=records, active_record=active_record)

@app.route('/check_in', methods=['POST'])
@login_required
def check_in():
    active_record = TimeRecord.query.filter_by(user_id=current_user.id, check_out=None).first()
    if active_record:
        flash('Ya tienes un registro activo', 'warning')
    else:
        record = TimeRecord(user_id=current_user.id, check_in=datetime.now())
        db.session.add(record)
        db.session.commit()
        flash('Registro de entrada exitoso', 'success')
    return redirect(url_for('index'))

@app.route('/check_out', methods=['POST'])
@login_required
def check_out():
    active_record = TimeRecord.query.filter_by(user_id=current_user.id, check_out=None).first()
    if active_record:
        active_record.check_out = datetime.now()
        db.session.commit()
        flash('Registro de salida exitoso', 'success')
    else:
        flash('No tienes un registro activo', 'warning')
    return redirect(url_for('index'))

@app.route('/login', methods=['GET', 'POST'])
def login():
    if current_user.is_authenticated:
        return redirect(url_for('index'))
        
    if request.method == 'POST':
        username = request.form.get('username')
        password = request.form.get('password')
        
        if not username or not password:
            flash('Por favor, introduce usuario y contraseña.', 'danger')
            return redirect(url_for('login'))
        
        user = User.query.filter_by(username=username).first()
        
        if user and bcrypt.check_password_hash(user.password, password):
            login_user(user)
            flash(f'Bienvenido/a, {user.name}', 'success')
            return redirect(url_for('index'))
        else:
            flash('Usuario o contraseña incorrectos.', 'danger')
            
    return render_template('login.html')

@app.route('/logout')
@login_required
def logout():
    logout_user()
    return redirect(url_for('login'))

@app.route('/privacy-policy')
def privacy_policy():
    return render_template('privacy_policy.html')

@app.route('/data-consent', methods=['GET', 'POST'])
@login_required
def data_consent():
    if request.method == 'POST':
        consent_type = request.form.get('consent_type')
        granted = request.form.get('granted') == 'true'
        
        consent = DataProcessingConsent(
            user_id=current_user.id,
            consent_type=consent_type,
            granted=granted,
            ip_address=request.remote_addr
        )
        
        if consent_type == 'general':
            current_user.data_consent = granted
            current_user.consent_date = datetime.utcnow() if granted else None
            
        db.session.add(consent)
        db.session.commit()
        
        flash('Preferencias de privacidad actualizadas', 'success')
        return redirect(url_for('privacy_settings'))
        
    return render_template('data_consent.html')

@app.route('/privacy-settings')
@login_required
def privacy_settings():
    return render_template('privacy_settings.html')

@app.route('/export-data')
@login_required
def export_data():
    # Get user data
    user_data = {
        'username': current_user.username,
        'name': current_user.name,
        'email': current_user.email,
        'records': []
    }
    
    # Get time records
    records = TimeRecord.query.filter_by(user_id=current_user.id).all()
    for record in records:
        user_data['records'].append({
            'check_in': record.check_in.isoformat(),
            'check_out': record.check_out.isoformat() if record.check_out else None
        })
    
    # Create JSON response
    return jsonify(user_data)

@app.route('/delete-data', methods=['POST'])
@login_required
def delete_data():
    if request.form.get('confirm') == 'true':
        # Delete time records
        TimeRecord.query.filter_by(user_id=current_user.id).delete()
        # Delete consent records
        DataProcessingConsent.query.filter_by(user_id=current_user.id).delete()
        # Delete user
        db.session.delete(current_user)
        db.session.commit()
        logout_user()
        flash('Tu cuenta y todos tus datos han sido eliminados', 'success')
        return redirect(url_for('login'))
    
    flash('Confirmación requerida para eliminar datos', 'error')
    return redirect(url_for('privacy_settings'))

@app.context_processor
def utility_processor():
    return {'now': datetime.now()}

if __name__ == '__main__':
    with app.app_context():
        db.create_all()
    app.run(debug=True)
