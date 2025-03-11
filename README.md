# Employee Time Tracker

Una aplicación web desarrollada con Flask para el seguimiento del tiempo de los empleados.

## Características

- Registro de entrada y salida de empleados
- Generación de informes de tiempo
- Panel de administración
- Gestión de usuarios
- Exportación de informes en PDF
- Políticas de privacidad y consentimiento de datos

## Tecnologías

- Python 3.x
- Flask
- SQLAlchemy
- Flask-Login
- ReportLab
- PostgreSQL (producción)
- SQLite (desarrollo)

## Instalación

1. Clonar el repositorio:
```bash
git clone https://github.com/IaEnjoyer/employee-time-tracker.git
cd employee-time-tracker
```

2. Crear un entorno virtual:
```bash
python -m venv venv
source venv/bin/activate  # En Windows: venv\Scripts\activate
```

3. Instalar dependencias:
```bash
pip install -r requirements.txt
```

4. Configurar variables de entorno:
Crear un archivo `.env` con:
```
FLASK_APP=app.py
FLASK_ENV=development
SECRET_KEY=tu_clave_secreta
```

5. Iniciar la aplicación:
```bash
flask run
```

## Licencia

Este proyecto está bajo la licencia MIT.
