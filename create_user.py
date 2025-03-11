from app import app, db, User, bcrypt
from datetime import datetime

def create_admin_user():
    with app.app_context():
        # Verificar si el usuario admin ya existe
        if User.query.filter_by(username='admin').first():
            print("El usuario admin ya existe.")
            return
        
        # Crear usuario administrador
        hashed_password = bcrypt.generate_password_hash('admin123').decode('utf-8')
        user = User(
            username='admin',
            password=hashed_password,
            name='Administrador',
            email='admin@empresa.com',
            data_consent=True,
            consent_date=datetime.utcnow(),
            data_retention_days=365,
            role='admin'
        )
        db.session.add(user)
        try:
            db.session.commit()
            print("Usuario admin creado exitosamente")
        except Exception as e:
            db.session.rollback()
            print(f"Error al crear usuario admin: {e}")

def create_employee():
    with app.app_context():
        # Verificar si el usuario ya existe
        if User.query.filter_by(username='empleado1').first():
            print("El usuario 'empleado1' ya existe.")
            return

        # Crear nuevo empleado
        hashed_password = bcrypt.generate_password_hash('empleado123').decode('utf-8')
        new_employee = User(
            username='empleado1',
            name='Juan Pérez',
            email='juan.perez@empresa.com',
            password=hashed_password,
            role='employee',
            data_consent=True,
            consent_date=datetime.utcnow(),
            data_retention_days=365
        )

        # Añadir y confirmar en la base de datos
        db.session.add(new_employee)
        try:
            db.session.commit()
            print("Usuario empleado creado exitosamente.")
        except Exception as e:
            db.session.rollback()
            print(f"Error al crear empleado: {e}")

if __name__ == '__main__':
    create_admin_user()
    create_employee()
