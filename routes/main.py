from flask import Blueprint, render_template, redirect, url_for, flash, request
from flask_login import login_required, current_user
from models import TimeRecord, db
from datetime import datetime

main = Blueprint('main', __name__)

@main.route('/')
@login_required
def index():
    if current_user.role == 'admin':
        return redirect(url_for('admin.manage_users'))
    records = TimeRecord.query.filter_by(user_id=current_user.id).order_by(TimeRecord.check_in.desc()).all()
    active_record = TimeRecord.query.filter_by(user_id=current_user.id, check_out=None).first()
    return render_template('index.html', records=records, active_record=active_record)

@main.route('/check_in', methods=['POST'])
@login_required
def check_in():
    if current_user.role == 'admin':
        flash('Los administradores no pueden registrar tiempo.', 'warning')
        return redirect(url_for('main.index'))
        
    active_record = TimeRecord.query.filter_by(user_id=current_user.id, check_out=None).first()
    if active_record:
        flash('Ya tienes un registro activo.', 'warning')
        return redirect(url_for('main.index'))
    
    new_record = TimeRecord(user_id=current_user.id)
    db.session.add(new_record)
    try:
        db.session.commit()
        flash('Entrada registrada correctamente.', 'success')
    except:
        db.session.rollback()
        flash('Error al registrar la entrada.', 'error')
    
    return redirect(url_for('main.index'))

@main.route('/check_out', methods=['POST'])
@login_required
def check_out():
    if current_user.role == 'admin':
        flash('Los administradores no pueden registrar tiempo.', 'warning')
        return redirect(url_for('main.index'))
        
    active_record = TimeRecord.query.filter_by(user_id=current_user.id, check_out=None).first()
    if not active_record:
        flash('No tienes ningún registro activo.', 'warning')
        return redirect(url_for('main.index'))
    
    active_record.check_out = datetime.utcnow()
    try:
        db.session.commit()
        flash('Salida registrada correctamente.', 'success')
    except:
        db.session.rollback()
        flash('Error al registrar la salida.', 'error')
    
    return redirect(url_for('main.index'))
