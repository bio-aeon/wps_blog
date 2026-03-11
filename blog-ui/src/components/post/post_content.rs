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
                    var container = document.querySelector('.post-content');
                    if (!container) return;

                    // Lazy loading and CLS prevention for images
                    container.querySelectorAll('img').forEach(function(img) {
                        if (!img.hasAttribute('loading')) {
                            img.setAttribute('loading', 'lazy');
                            img.setAttribute('decoding', 'async');
                        }
                    });

                    // Conditionally load Prism.js only if code blocks exist
                    var hasCode = container.querySelector('pre code');
                    if (!hasCode) return;
                    if (typeof Prism !== 'undefined') {
                        Prism.highlightAllUnder(container);
                        return;
                    }

                    var prismBase = 'https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0';
                    var langs = [
                        'rust','java','scala','python','sql','bash',
                        'typescript','json','yaml','toml','haskell','idris'
                    ];

                    var core = document.createElement('script');
                    core.src = prismBase + '/prism.min.js';
                    core.setAttribute('data-manual', '');
                    core.onload = function() {
                        var loaded = 0;
                        langs.forEach(function(lang) {
                            var s = document.createElement('script');
                            s.src = prismBase + '/components/prism-' + lang + '.min.js';
                            s.onload = function() {
                                loaded++;
                                if (loaded === langs.length) {
                                    Prism.highlightAllUnder(container);
                                }
                            };
                            document.head.appendChild(s);
                        });
                    };
                    document.head.appendChild(core);
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
