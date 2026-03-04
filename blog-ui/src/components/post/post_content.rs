use leptos::prelude::*;

#[component]
pub fn PostContent(html_content: String) -> impl IntoView {
    // After content is mounted, apply syntax highlighting and optimize images
    #[cfg(feature = "hydrate")]
    {
        Effect::new(move |_| {
            use leptos::web_sys;
            use wasm_bindgen::prelude::*;

            #[wasm_bindgen(inline_js = "
                export function enhance_post_content() {
                    // Syntax highlighting via Prism.js (if loaded)
                    if (typeof Prism !== 'undefined') {
                        var container = document.querySelector('.post-content');
                        if (container) {
                            Prism.highlightAllUnder(container);
                        }
                    }
                    // Lazy loading for images
                    document.querySelectorAll('.post-content img').forEach(img => {
                        if (!img.hasAttribute('loading')) {
                            img.setAttribute('loading', 'lazy');
                            img.setAttribute('decoding', 'async');
                        }
                    });
                }
            ")]
            extern "C" {
                fn enhance_post_content();
            }

            // Use requestAnimationFrame to wait for DOM update
            let cb = Closure::once_into_js(move || {
                enhance_post_content();
            });
            web_sys::window()
                .unwrap()
                .request_animation_frame(cb.as_ref().unchecked_ref())
                .ok();
        });
    }

    view! {
        <div class="post-content" inner_html=html_content></div>
    }
}
