# -*- coding: utf-8 -*-
from django import forms
from .models import Comment


class CommentForm(forms.ModelForm):
    name = forms.CharField(max_length=255, widget=forms.TextInput(attrs={'class': 'emp', 'data-hint': u'Имя'}))
    email = forms.EmailField(widget=forms.TextInput(attrs={'class': 'emp', 'data-hint': u'your@email'}))
    text = forms.CharField(widget=forms.Textarea(attrs={'class': 'emp', 'data-hint': u'Текст комментария'}))
    check = forms.RegexField(regex=r'^$', required=False, widget=forms.HiddenInput,
                             error_message=u'Ошибка проверки')

    class Meta:
        model = Comment
        fields = ('name', 'email', 'text', 'post', 'parent', 'check')
        widgets = {
            'post': forms.HiddenInput,
            'parent': forms.HiddenInput,
        }