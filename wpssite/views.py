# -*- coding: utf-8 -*-
import inspect
from django.shortcuts import render_to_response
from django.template import RequestContext
from django.http import Http404
from django.utils.translation import ugettext as _
from django.views.generic import ListView


class CallableClassViewException(Http404): pass


class CallableClassView(object):
    def __new__(cls, request, *args, **kwargs):
        obj = super(CallableClassView, cls).__new__(cls)
        return obj(request, *args, **kwargs)

    def __call__(self, request, *args, **kwargs):
        #запоминаем приложение, view, действие
        self._app = inspect.getmodule(self).__name__.split('.')[-2]
        self._view = self.__class__.__name__
        self._action = kwargs.pop('action', 'index')
        self._request = request

        #есть ли требуемый атрибут и является ли он public
        if hasattr(self, self._action) and not self._action.startswith('_'):
            handler = getattr(self, self._action)
            if inspect.ismethod(handler): #если это метод
                return handler(*args, **kwargs) #вызываем действие
            else:
                raise CallableClassViewException(u'"%s" is not a method' % self._action)
        else:
            raise CallableClassViewException(u'"%s" have not public method "%s"' % (self._view, self._action))

    def _add_route_info(self, params):
        params['route'] = {'app': self._app, 'view': self._view, 'action': self._action}
        return params

    def _render_view(self, view_class, **kwargs):
        extra_params = kwargs.pop('extra_params', {})
        view = view_class.as_view(**kwargs)
        return view(self._request, **self._add_route_info(extra_params))

    def _render(self, template_name, context={}):
        return render_to_response(template_name, self._add_route_info(context),
                                  context_instance=RequestContext(self._request))

    def _load_model(self, cls, id):
        try:
            model = cls.objects.get(pk=id)
        except cls.DoesNotExist:
            raise CallableClassViewException(u'Запись не существует')
        else:
            return model


class AdaptiveListView(ListView):

    def get(self, request, *args, **kwargs):
        self.object_list = self.get_queryset()
        allow_empty = self.get_allow_empty()
        if not allow_empty and len(self.object_list) == 0:
            raise Http404(_(u"Empty list and '%(class_name)s.allow_empty' is False.")
            % {'class_name': self.__class__.__name__})
        context = self.get_context_data(object_list=self.object_list, **kwargs)
        return self.render_to_response(context)