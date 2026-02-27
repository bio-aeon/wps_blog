use crate::api::get_about;
use crate::components::about::*;
use crate::components::common::{ErrorDisplay, GenericSkeleton};
use leptos::prelude::*;
use leptos_meta::Title;

#[component]
pub fn AboutPage() -> impl IntoView {
    let about_resource = Resource::new(|| (), |_| get_about());

    view! {
        <Title text="About - WPS Blog"/>
        <Suspense fallback=move || {
            view! { <GenericSkeleton/> }
        }>
            {move || Suspend::new(async move {
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
                                                <h2 class="about-section-title">"Skills"</h2>
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
                                                <h2 class="about-section-title">"Experience"</h2>
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
                                back_url="/".to_string()
                                back_label="← Back to Home".to_string()
                            />
                        }
                            .into_any()
                    }
                }
            })}
        </Suspense>
    }
}
