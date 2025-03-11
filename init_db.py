from app import db, app
from create_user import create_admin_user

def init_database():
    with app.app_context():
        # Crear todas las tablas
        db.create_all()
        
        # Crear usuario administrador inicial
        create_admin_user()
        
        print("Base de datos inicializada correctamente.")

if __name__ == '__main__':
    init_database()
