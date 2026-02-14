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
            <div class="container">
                <a class="logo" href="/">"WPS Blog"</a>
                <nav
                    class="main-nav"
                    class:open=move || mobile_menu_open.get()
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
                        "Blog"
                    </a>
                    <a
                        href="/tags"
                        class:active=move || pathname().starts_with("/tags")
                        on:click=close_menu
                    >
                        "Tags"
                    </a>
                    <a
                        href="/pages/about"
                        class:active=move || pathname() == "/pages/about"
                        on:click=close_menu
                    >
                        "About"
                    </a>
                </nav>
                <a class="search-trigger" href="/search">"Search"</a>
                <button
                    class="mobile-menu-toggle"
                    aria-label="Toggle menu"
                    aria-expanded=move || mobile_menu_open.get().to_string()
                    on:click=toggle_menu
                >
                    <span class="hamburger"></span>
                </button>
            </div>
        </header>
    }
}
