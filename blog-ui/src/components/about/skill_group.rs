use crate::api::types::SkillCategoryResult;
use crate::components::about::SkillBar;
use leptos::prelude::*;

#[component]
pub fn SkillGroup(category: SkillCategoryResult) -> impl IntoView {
    view! {
        <div class="skill-group">
            <h3 class="skill-group-title">{category.category}</h3>
            <div class="skill-group-items">
                {category
                    .skills
                    .into_iter()
                    .map(|skill| {
                        let icon = skill.icon;
                        view! {
                            <SkillBar
                                name=skill.name
                                proficiency=skill.proficiency
                                icon=icon.unwrap_or_default()
                            />
                        }
                    })
                    .collect_view()}
            </div>
        </div>
    }
}
