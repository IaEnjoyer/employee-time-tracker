from app import app, db, User, bcrypt
from datetime import datetime

with app.app_context():
    # Drop all tables and recreate them
    db.drop_all()
    db.create_all()
    
    # Create admin user with all required fields
    user = User(
        username='admin',
        password='admin123',
        name='Administrador',
        email='admin@empresa.com',
        data_consent=True,
        consent_date=datetime.utcnow(),
        data_retention_days=365
    )
    db.session.add(user)
    db.session.commit()
    print("Usuario creado exitosamente")

def create_employee():
    with app.app_context():
        # Check if user already exists
        if User.query.filter_by(username='empleado1').first():
            print("El usuario 'empleado1' ya existe.")
            return

        # Create a new employee user
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

        # Add and commit to database
        db.session.add(new_employee)
        db.session.commit()
        print("Usuario empleado creado exitosamente.")

if __name__ == '__main__':
    create_employee()
