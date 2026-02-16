use leptos::prelude::*;

const DEBOUNCE_MS: f64 = 300.0;

#[component]
pub fn SearchBar(
    #[prop(optional)] initial_query: Option<String>,
    on_search: Callback<String>,
) -> impl IntoView {
    let (query, set_query) = signal(initial_query.unwrap_or_default());
    let (timer_id, set_timer_id) = signal(Option::<f64>::None);

    let on_search_submit = on_search.clone();
    let handle_submit = move |ev: leptos::ev::SubmitEvent| {
        ev.prevent_default();
        if let Some(id) = timer_id.get() {
            clear_timeout(id);
            set_timer_id.set(None);
        }
        on_search_submit.run(query.get());
    };

    let handle_input = move |ev: leptos::ev::Event| {
        let value = event_target_value(&ev);
        set_query.set(value.clone());

        if let Some(id) = timer_id.get() {
            clear_timeout(id);
        }

        let on_search = on_search.clone();
        let id = set_timeout(
            move || {
                on_search.run(value);
            },
            DEBOUNCE_MS,
        );
        set_timer_id.set(Some(id));
    };

    let handle_clear = move |_| {
        if let Some(id) = timer_id.get() {
            clear_timeout(id);
            set_timer_id.set(None);
        }
        set_query.set(String::new());
        on_search.run(String::new());
    };

    view! {
        <form class="search-bar" on:submit=handle_submit>
            <div class="search-input-wrapper">
                <input
                    type="text"
                    class="search-input"
                    placeholder="Search posts..."
                    prop:value=move || query.get()
                    on:input=handle_input
                />
                <Show when=move || !query.get().is_empty()>
                    <button type="button" class="search-clear" on:click=handle_clear>
                        "×"
                    </button>
                </Show>
            </div>
            <button type="submit" class="search-submit">"Search"</button>
        </form>
    }
}

#[cfg(target_arch = "wasm32")]
fn set_timeout(callback: impl FnOnce() + 'static, ms: f64) -> f64 {
    use wasm_bindgen::prelude::*;

    #[wasm_bindgen]
    extern "C" {
        #[wasm_bindgen(js_name = setTimeout)]
        fn set_timeout_js(closure: &Closure<dyn FnMut()>, ms: f64) -> f64;
    }

    let closure = Closure::once(callback);
    let id = set_timeout_js(&closure, ms);
    closure.forget();
    id
}

#[cfg(not(target_arch = "wasm32"))]
fn set_timeout(_callback: impl FnOnce() + 'static, _ms: f64) -> f64 {
    0.0
}

#[cfg(target_arch = "wasm32")]
fn clear_timeout(id: f64) {
    use wasm_bindgen::prelude::*;

    #[wasm_bindgen]
    extern "C" {
        #[wasm_bindgen(js_name = clearTimeout)]
        fn clear_timeout_js(id: f64);
    }

    clear_timeout_js(id);
}

#[cfg(not(target_arch = "wasm32"))]
fn clear_timeout(_id: f64) {}
