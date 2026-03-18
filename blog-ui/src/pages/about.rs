use crate::api::get_about;
use crate::components::about::*;
use crate::components::common::{ErrorDisplay, GenericSkeleton};
use crate::i18n::{lang_href, use_language, use_translations};
use leptos::prelude::*;
use leptos_meta::Title;

#[component]
pub fn AboutPage() -> impl IntoView {
    let lang = use_language();
    let t = use_translations();
    let about_resource = Resource::new(|| (), |_| get_about());

    view! {
        <Title text=format!("{} - WPS Blog", t.about.title)/>
        <Suspense fallback=move || {
            view! { <GenericSkeleton/> }
        }>
            {move || {
                let back_label = t.common.back_home;
                let lang = lang.clone();
                Suspend::new(async move {
                    match about_resource.await {
                        Ok(about) => {
                            let has_skills = !about.skills.is_empty();
                            let has_experiences = !about.experiences.is_empty();
                            let has_social_links = !about.social_links.is_empty();
                            let has_bio = !about.profile.bio.is_empty();

                            view! {
                                <article class="about-page">
                                    <ProfileHeader profile=about.profile.clone()/>
                                    {has_bio
                                        .then(|| {
                                            view! {
                                                <section class="about-bio">
                                                    <div inner_html=about.profile.bio.clone()></div>
                                                </section>
                                            }
                                        })}
                                    {has_skills
                                        .then(|| {
                                            view! {
                                                <section class="about-section">
                                                    <h2 class="about-section-title">{t.about.skills}</h2>
                                                    <div class="skills-grid">
                                                        {about
                                                            .skills
                                                            .into_iter()
                                                            .map(|cat| {
                                                                view! { <SkillGroup category=cat/> }
                                                            })
                                                            .collect_view()}
                                                    </div>
                                                </section>
                                            }
                                        })}
                                    {has_experiences
                                        .then(|| {
                                            view! {
                                                <section class="about-section">
                                                    <h2 class="about-section-title">{t.about.experience}</h2>
                                                    <ExperienceTimeline experiences=about.experiences/>
                                                </section>
                                            }
                                        })}
                                    {has_social_links
                                        .then(|| {
                                            view! {
                                                <section class="about-section">
                                                    <h2 class="about-section-title">"Connect"</h2>
                                                    <SocialLinks links=about.social_links/>
                                                </section>
                                            }
                                        })}
                                </article>
                            }
                                .into_any()
                        }
                        Err(e) => {
                            view! {
                                <ErrorDisplay
                                    title="Failed to load page".to_string()
                                    message=e.to_string()
                                    back_url=lang_href(&lang, "/")
                                    back_label=back_label.to_string()
                                />
                            }
                                .into_any()
                        }
                    }
                })
            }}
        </Suspense>
    }
}
