# Wps Blog

My simple blog on Django with search, tags and tree comments with rating.

## Run locally

```bash
pip install -r requirements.txt
python manage.py migrate --settings=wps_blog.settings --configuration=Dev
python manage.py runserver --settings=wps_blog.settings --configuration=Dev
```

## Run tests

```bash
python manage.py test --settings=wps_blog.settings --configuration=Dev
```
