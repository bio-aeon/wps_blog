use crate::api::types::ExperienceResult;
use crate::components::about::ExperienceItem;
use leptos::prelude::*;

#[component]
pub fn ExperienceTimeline(experiences: Vec<ExperienceResult>) -> impl IntoView {
    view! {
        <div class="experience-timeline">
            {experiences
                .into_iter()
                .map(|exp| {
                    view! { <ExperienceItem experience=exp/> }
                })
                .collect_view()}
        </div>
    }
}
