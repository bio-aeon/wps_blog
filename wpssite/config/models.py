# -*- coding: utf-8 -*-
from django.db import models


class ParamsManager(models.Manager):
    params = {}

    def get_class(self):
        return self.__class__

    def get(self, param_name, preselect_all=False):
        """Get value of param with given name

        :param param_name: Param name to use.
        :type param_name: str.
        :param preselect_all: If preselect all params.
        :type preselect_all: bool.
        :rtype: str.

        """
        try:
            if self.get_class().params:
                param = self.get_class().params[param_name]
            else:
                if preselect_all:
                    rows = self.all()
                    params = {}
                    for row in rows:
                        params[row.name] = row.value

                    self.get_class().params = params
                    param = params[param_name]
                else:
                    param = super(self.get_class(), self).get(name=param_name).value
        except (KeyError, self.model.DoesNotExist):
            param = ''
        return param


class Config(models.Model):
    name = models.CharField(max_length=255, unique=True, blank=False, null=False)
    value = models.CharField(max_length=255, blank=False, null=False)
    comment = models.TextField(blank=True, null=True)

    params = ParamsManager()
    objects = models.Manager()

    class Meta:
        verbose_name = u'Параметр'
        verbose_name_plural = u'Параметры'

    def __unicode__(self):
        return self.name