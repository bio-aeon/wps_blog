$(document).ready(function() {
	Cufon.now();
	Cufon.replace("h1.m_title a", {
		fontFamily: "PT Sans",
		color: "-linear-gradient(#1397cf, #1397cf, #1397cf, #b0d3e1, #1397cf)"
	});
	
//	Cufon.replace(".s_title span.f", {
//		fontFamily: "Lucida Console"
//	});
	
	Cufon.replace(".s_title span.s", {
		fontFamily: "Myriad Pro"
	});

    $("[data-id=search-field]").val($("[data-id=search-field]").attr("data-hint"));
    $("[data-id=search-field]").focus(function() {
        if($(this).hasClass("emp")) {
            $(this).val("");
            $(this).removeClass("emp");
        }
    });

    $("[data-id=search-field]").blur(function() {
        if($(this).val() == "") {
            $(this).addClass("emp");
            $(this).val($(this).attr("data-hint"));
        }
        else {
            $(this).removeClass("emp");
        }
    });

    $("[data-id=search-button]").click(function() {
        if($("[data-id=search-field]").hasClass("emp")) {
            $("[data-id=search-field]").val("");
        }

        $(this).parent().submit();
    });

    hljs.initHighlightingOnLoad();
});