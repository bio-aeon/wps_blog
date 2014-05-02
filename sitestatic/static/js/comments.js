$(document).ready(function() {
    (function()
    {
        var te = $("[data-id=comments]");

        $(te).on("focus", "[data-type=comment-form] input[type=text], textarea", function() {
            if($(this).hasClass("emp")) {
                $(this).val("");
                $(this).removeClass("emp");
            }
        });

        $(te).on("blur", "[data-type=comment-form] input[type=text], textarea", function() {
            if($(this).val() == "") {
                $(this).addClass("emp");
                $(this).val($(this).attr("data-hint"));
            }
            else {
                $(this).removeClass("emp");
            }
        });

        $(te).on("click", "[data-type=comment-reply]", function() {
            var formParent = $(this).closest("[data-type=comment]").find("[data-type=comment-form]").first();
            var unfolder = $(this).closest("[data-type=comment]").find(".unfolder").first();
            var form = $(formParent).children("form");
            if(!form.length) {
                clonePrimaryForm(formParent, $(this).attr("data-comment-id"));
                $(unfolder).show();
            }
            else {
                $(formParent).toggle();
                $(unfolder).toggle();
            }
        });

        $(te).on("hover", "[data-type=comment-reply]", function(event) {
            switch (event.type) {
                case "mouseenter":
                    $(this).closest("[data-type=comment]").addClass("hovered");
                    break;
                case "mouseleave":
                    $(this).closest("[data-type=comment]").removeClass("hovered");
                    break;
            }
        });

        $(te).on("click", "[data-type=change-rate]", function() {
            var self = this
            var csrfName = "csrfmiddlewaretoken";
            var csrf = $("[data-id=primary-form] form").find("input[name=" + csrfName + "]").val();
            $.ajax({
                type: "POST",
                url: $(this).attr("href"),
                data: csrfName + "=" + csrf,
                dataType: "json",
                success: function(response) {
                    if(response.rating) {
                        $(self).closest("[data-type=comment]").find("[data-type=comment-rating]")
                            .first()
                            .text(response.rating);
                    }
                    else if(response.error) {
                        noty({text: response.error, layout: "center", closeWith: ['click', 'button'], timeout: 2500});
                    }

                },
            });

            return false;
        });

        $(te).on("click", "[data-type=send-comment]", function() {
            var self = this;
            var form = $(this).closest("form");
            var formParent = $(form).parent();
            var action = $(formParent).attr("data-action");
            $(form).find("[data-type=loading-box]").addClass("active")
            clearEmpty(form);
            $.ajax({
                type: "POST",
                url: $(form).attr("action"),
                data: $(form).serialize(),
                dataType: "json",
                success: function(response) {
                    $(form).find('.error').remove();
                    if(response.result == "success") {
                        switch (action) {
                            case "add":
                                $("[data-id=comments-list]").append(response.data);
                                $(formParent).empty();
                                clonePrimaryForm(formParent)
                                break;
                            case "reply":
                                var unfolder = $(self).closest("[data-type=sub-comments]").find(".unfolder").first();
                                $(self).closest("[data-type=comment]")
                                       .find("[data-type=sub-comments-list]")
                                       .first()
                                       .append(response.data);

                                $(formParent).empty();
                                $(unfolder).toggle();
                                break;
                        }

                        noty({
                            text: "Комментарий успешно добавлен.<br/><a href='#comment-"
                                  + response.id
                                  + "'>Перейти к комментарию</a>",
                            layout: "center",
                            timeout: 5000,
                            closeWith: ['click', 'button']
                        });
                    }
                    else {
                        displayFormErrors(form, response.data);
                        $(form).find("[data-type=loading-box]").removeClass("active");
                    }
                }
            });

            fillHints(form);
        });

        clonePrimaryForm("[data-type=comment-form][data-action=add]");

        /*utils*/

        function clonePrimaryForm(target, parent)
        {
            $("[data-id=primary-form] form").clone().appendTo(target);
            if(parent) {
                $(target).children("form").find("input[name=parent]").val(parent);
            }

            fillHints($(target).find("form"));
        }

        function clearEmpty(form)
        {
            $(form).find(".emp").val("");
        }

        function fillHints(form)
        {
            $.each($(form).find(".emp"), function(key, elem) {
                $(elem).val($(elem).attr("data-hint"));
            });
        }

        function displayFormErrors(form, errors) {
            for (var k in errors) {
                $(form).find('input[name=' + k + '], textarea[name=' + k + ']').after('<div class="error">' + errors[k] + '</div>');
            }
        }
    })();
});