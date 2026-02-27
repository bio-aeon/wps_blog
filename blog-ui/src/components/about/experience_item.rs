use crate::api::types::ExperienceResult;
use leptos::prelude::*;

/// Format a "YYYY-MM-DD" date to "Month YYYY" display format.
pub fn format_experience_date(date_str: &str) -> String {
    chrono::NaiveDate::parse_from_str(date_str, "%Y-%m-%d")
        .map(|d| d.format("%B %Y").to_string())
        .unwrap_or_else(|_| date_str.to_string())
}

#[component]
pub fn ExperienceItem(experience: ExperienceResult) -> impl IntoView {
    let date_range = {
        let start = format_experience_date(&experience.start_date);
        let end = experience
            .end_date
            .as_deref()
            .map(format_experience_date)
            .unwrap_or_else(|| "Present".to_string());
        format!("{} – {}", start, end)
    };

    view! {
        <div class="timeline-item">
            <div class="timeline-marker"></div>
            <div class="timeline-content">
                <h3 class="timeline-position">{experience.position}</h3>
                <div class="timeline-company">
                    {match experience.company_url.as_ref() {
                        Some(url) => {
                            view! {
                                <a href=url.clone() target="_blank" rel="noopener">
                                    {experience.company.clone()}
                                </a>
                            }
                                .into_any()
                        }
                        None => view! { <span>{experience.company.clone()}</span> }.into_any(),
                    }}
                    {experience
                        .location
                        .as_ref()
                        .map(|loc| {
                            view! { <span class="timeline-location">" · "{loc.clone()}</span> }
                        })}
                </div>
                <div class="timeline-date">{date_range}</div>
                <div class="timeline-description" inner_html=experience.description></div>
            </div>
        </div>
    }
}
