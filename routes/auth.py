from flask import Blueprint, render_template, redirect, url_for, flash, request
from flask_login import login_user, logout_user, login_required, current_user
from werkzeug.security import check_password_hash
from models import User

auth = Blueprint('auth', __name__)

@auth.route('/login', methods=['GET', 'POST'])
def login():
    if current_user.is_authenticated:
        if current_user.role == 'admin':
            return redirect(url_for('admin.manage_users'))
        return redirect(url_for('main.index'))
        
    if request.method == 'POST':
        username = request.form.get('username')
        password = request.form.get('password')
        
        if not username or not password:
            flash('Por favor, introduce usuario y contraseña.', 'danger')
            return redirect(url_for('auth.login'))
        
        user = User.query.filter_by(username=username).first()
        
        if user and check_password_hash(pwhash=user.password, password=password):
            login_user(user)
            flash('Has iniciado sesión correctamente', 'success')
            if user.role == 'admin':
                return redirect(url_for('admin.manage_users'))
            return redirect(url_for('main.index'))
        else:
            flash('Usuario o contraseña incorrectos', 'error')
    
    return render_template('login.html')

@auth.route('/logout')
@login_required
def logout():
    logout_user()
    flash('Has cerrado sesión correctamente', 'success')
    return redirect(url_for('auth.login'))
