use leptos::prelude::*;
use leptos_router::hooks::use_location;

#[component]
pub fn Header() -> impl IntoView {
    let location = use_location();
    let pathname = move || location.pathname.get();
    let mobile_menu_open = RwSignal::new(false);

    let toggle_menu = move |_| {
        *mobile_menu_open.write() = !mobile_menu_open.get();
    };

    let close_menu = move |_| {
        mobile_menu_open.set(false);
    };

    view! {
        <header class="site-header">
            <div class="header-inner">
                <a class="logo" href="/">"WPS Blog"</a>
                <nav
                    class="header-nav"
                    id="main-nav"
                    aria-label="Main navigation"
                    class:open=move || mobile_menu_open.get()
                    on:keydown=move |ev: leptos::ev::KeyboardEvent| {
                        if ev.key() == "Escape" {
                            mobile_menu_open.set(false);
                        }
                    }
                >
                    <a
                        href="/"
                        class:active=move || pathname() == "/"
                        on:click=close_menu
                    >
                        "Home"
                    </a>
                    <a
                        href="/posts"
                        class:active=move || pathname().starts_with("/posts")
                        on:click=close_menu
                    >
                        "Posts"
                    </a>
                    <a
                        href="/tags"
                        class:active=move || pathname().starts_with("/tags")
                        on:click=close_menu
                    >
                        "Tags"
                    </a>
                    <a
                        href="/about"
                        class:active=move || pathname() == "/about"
                        on:click=close_menu
                    >
                        "About"
                    </a>
                    <a
                        href="/contact"
                        class:active=move || pathname() == "/contact"
                        on:click=close_menu
                    >
                        "Contact"
                    </a>
                    <div class="header-search" role="search">
                        <input
                            type="search"
                            placeholder="Search..."
                            aria-label="Search posts"
                        />
                    </div>
                </nav>
                <button
                    class="mobile-menu-toggle"
                    aria-label="Toggle menu"
                    aria-expanded=move || mobile_menu_open.get().to_string()
                    aria-controls="main-nav"
                    on:click=toggle_menu
                >
                    <span class="hamburger" aria-hidden="true"></span>
                </button>
            </div>
        </header>
    }
}
