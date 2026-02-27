use leptos::prelude::*;

#[component]
pub fn SkillBar(
    #[prop(into)] name: String,
    proficiency: i32,
    #[prop(into, optional)] icon: String,
) -> impl IntoView {
    view! {
        <div class="skill-bar">
            <div class="skill-bar-header">
                {(!icon.is_empty())
                    .then(|| view! { <span class="skill-icon">{icon}</span> })}
                <span class="skill-name">{name}</span>
                <span class="skill-percent">{proficiency}"%"</span>
            </div>
            <div class="skill-bar-track">
                <div class="skill-bar-fill" style=format!("width: {}%", proficiency)></div>
            </div>
        </div>
    }
}
