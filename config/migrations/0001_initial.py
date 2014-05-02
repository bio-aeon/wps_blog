# -*- coding: utf-8 -*-
import datetime
from south.db import db
from south.v2 import SchemaMigration
from django.db import models


class Migration(SchemaMigration):

    def forwards(self, orm):
        # Adding model 'Config'
        db.create_table('config_config', (
            ('id', self.gf('django.db.models.fields.AutoField')(primary_key=True)),
            ('name', self.gf('django.db.models.fields.CharField')(unique=True, max_length=255)),
            ('value', self.gf('django.db.models.fields.CharField')(max_length=255)),
            ('comment', self.gf('django.db.models.fields.TextField')(null=True)),
        ))
        db.send_create_signal('config', ['Config'])


    def backwards(self, orm):
        # Deleting model 'Config'
        db.delete_table('config_config')


    models = {
        'config.config': {
            'Meta': {'object_name': 'Config'},
            'comment': ('django.db.models.fields.TextField', [], {'null': 'True'}),
            'id': ('django.db.models.fields.AutoField', [], {'primary_key': 'True'}),
            'name': ('django.db.models.fields.CharField', [], {'unique': 'True', 'max_length': '255'}),
            'value': ('django.db.models.fields.CharField', [], {'max_length': '255'})
        }
    }

    complete_apps = ['config']