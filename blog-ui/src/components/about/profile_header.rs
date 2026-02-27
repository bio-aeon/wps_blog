use crate::api::types::ProfileResult;
use leptos::prelude::*;

#[component]
pub fn ProfileHeader(profile: ProfileResult) -> impl IntoView {
    view! {
        <div class="profile-header">
            <img
                class="profile-photo"
                src=profile.photo_url.clone()
                alt=format!("Photo of {}", &profile.name)
            />
            <div class="profile-info">
                <h1 class="profile-name">{profile.name}</h1>
                <p class="profile-title">{profile.title}</p>
                {(!profile.resume_url.is_empty())
                    .then(|| {
                        view! {
                            <a href=profile.resume_url.clone() class="profile-resume-link" download>
                                "Download Resume"
                            </a>
                        }
                    })}
            </div>
        </div>
    }
}
