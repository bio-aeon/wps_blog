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
                    var langSri = {
                        'rust':       'sha384-JyDgFjMbyrE/TGiEUSXW3CLjQOySrsoiUNAlXTFdIsr/XUfaB7E+eYlR+tGQ9bCO',
                        'java':       'sha384-DioAMZB4yk91W6LuFit5wJDh8c5Ov09f/MBvja94y0PodMqTpTZeBeejqpRUru7D',
                        'scala':      'sha384-Ny05Z+BdncAWg13NKXaiXEbxcdi9Ej7lvfA0H405vGxx3gyoh1cc64pNrXx9jN/y',
                        'python':     'sha384-WJdEkJKrbsqw0evQ4GB6mlsKe5cGTxBOw4KAEIa52ZLB7DDpliGkwdme/HMa5n1m',
                        'sql':        'sha384-/MKWdycCDliku23mP5sYXbZNuXrzgmQO/jsVxwPFn99dVOaXRyKsqDjarqpueGAp',
                        'bash':       'sha384-9WmlN8ABpoFSSHvBGGjhvB3E/D8UkNB9HpLJjBQFC2VSQsM1odiQDv4NbEo+7l15',
                        'typescript': 'sha384-PeOqKNW/piETaCg8rqKFy+Pm6KEk7e36/5YZE5XO/OaFdO+/Aw3O8qZ9qDPKVUgx',
                        'json':       'sha384-RhrmFFMb0ZCHImjFMpR/UE3VEtIVTCtNrtKQqXCzqXZNJala02N3UbVhi+qzw3CY',
                        'yaml':       'sha384-AKAiycghK0jDCjD+aavMHzDkLzRR7Yzcwh3+xL/295cvyVMe+cxQfyQC8xxGGcI8',
                        'toml':       'sha384-Uh6n44GRSQeQSMIIfAjlbqojWR7F5KALTHNsspuLDrNCsXpDPRdZbJ5A42AP/cA4',
                        'haskell':    'sha384-sybwp8DLY2QpJ1Cwga2P6wyppZICCcuahD9IxUFS12CqhCOq2TIWUyd4BuD0MZgO',
                        'idris':      'sha384-amoTET8h6aBgr1Anx3IeiOFml2EeQwNgTEYrLPJNnypbaA59whMmT5i7Lay9wgvw'
                    };
                    var coreSri = 'sha384-06z5D//U/xpvxZHuUz92xBvq3DqBBFi7Up53HRrbV7Jlv7Yvh/MZ7oenfUe9iCEt';

                    var core = document.createElement('script');
                    core.src = prismBase + '/prism.min.js';
                    core.integrity = coreSri;
                    core.crossOrigin = 'anonymous';
                    core.setAttribute('data-manual', '');
                    core.onload = function() {
                        var loaded = 0;
                        var langs = Object.keys(langSri);
                        langs.forEach(function(lang) {
                            var s = document.createElement('script');
                            s.src = prismBase + '/components/prism-' + lang + '.min.js';
                            s.integrity = langSri[lang];
                            s.crossOrigin = 'anonymous';
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
