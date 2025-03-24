from flask import Blueprint, render_template, redirect, url_for, flash, request, send_file
from flask_login import login_required, current_user
from werkzeug.security import generate_password_hash
from models import User, Schedule, TimeRecord, Vacation, SickLeave, db
from datetime import datetime
import pandas as pd
import io

admin = Blueprint('admin', __name__)

def admin_required(f):
    def decorated_function(*args, **kwargs):
        if not current_user.is_authenticated or current_user.role != 'admin':
            flash('No tienes permisos para acceder a esta página.', 'danger')
            return redirect(url_for('main.index'))
        return f(*args, **kwargs)
    decorated_function.__name__ = f.__name__
    return decorated_function

@admin.route('/manage_users')
@login_required
@admin_required
def manage_users():
    users = User.query.filter(User.role != 'admin').all()
    return render_template('admin/users.html', users=users)

@admin.route('/create_user', methods=['GET', 'POST'])
@login_required
@admin_required
def create_user():
    if request.method == 'POST':
        username = request.form.get('username')
        password = request.form.get('password')
        name = request.form.get('name')
        email = request.form.get('email')
        
        if User.query.filter_by(username=username).first():
            flash('El nombre de usuario ya existe.', 'danger')
            return redirect(url_for('admin.create_user'))
            
        if User.query.filter_by(email=email).first():
            flash('El email ya está registrado.', 'danger')
            return redirect(url_for('admin.create_user'))
        
        hashed_password = generate_password_hash(password, method='pbkdf2:sha256')
        new_user = User(
            username=username,
            password=hashed_password,
            name=name,
            email=email,
            role='user',
            data_consent=True,
            consent_date=datetime.utcnow(),
            data_retention_days=365
        )
        
        db.session.add(new_user)
        try:
            db.session.commit()
            flash('Usuario creado exitosamente.', 'success')
            return redirect(url_for('admin.manage_users'))
        except Exception as e:
            db.session.rollback()
            flash('Error al crear el usuario.', 'danger')
            return redirect(url_for('admin.create_user'))
    
    return render_template('admin/create_user.html')

@admin.route('/schedules', methods=['GET', 'POST'])
@login_required
@admin_required
def manage_schedules():
    if request.method == 'POST':
        user_id = request.form.get('user_id')
        start_time = request.form.get('start_time')
        end_time = request.form.get('end_time')
        days = request.form.getlist('days[]')
        
        if not all([user_id, start_time, end_time, days]):
            flash('Todos los campos son requeridos.', 'danger')
            return redirect(url_for('admin.manage_schedules'))
        
        schedule = Schedule.query.filter_by(user_id=user_id).first()
        if not schedule:
            schedule = Schedule(user_id=user_id)
        
        schedule.start_time = start_time
        schedule.end_time = end_time
        schedule.workdays = ','.join(days)
        
        try:
            if not schedule.id:
                db.session.add(schedule)
            db.session.commit()
            flash('Horario actualizado correctamente.', 'success')
        except:
            db.session.rollback()
            flash('Error al actualizar el horario.', 'danger')
        
        return redirect(url_for('admin.manage_schedules'))
    
    users = User.query.filter_by(role='user').all()
    return render_template('admin/schedules.html', users=users)

@admin.route('/reports')
@login_required
@admin_required
def reports():
    users = User.query.filter_by(role='user').all()
    return render_template('admin/reports.html', users=users)

@admin.route('/export_company_report')
@login_required
@admin_required
def export_company_report():
    records = TimeRecord.query.join(User).filter(User.role == 'user').all()
    
    data = []
    for record in records:
        data.append({
            'Usuario': record.user.name,
            'Entrada': record.check_in.strftime('%Y-%m-%d %H:%M:%S'),
            'Salida': record.check_out.strftime('%Y-%m-%d %H:%M:%S') if record.check_out else 'En curso',
            'Horas': round((record.check_out - record.check_in).total_seconds() / 3600, 2) if record.check_out else 0
        })
    
    df = pd.DataFrame(data)
    output = io.BytesIO()
    df.to_excel(output, index=False)
    output.seek(0)
    
    return send_file(
        output,
        mimetype='application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        as_attachment=True,
        download_name=f'reporte_empresa_{datetime.now().strftime("%Y%m%d")}.xlsx'
    )
