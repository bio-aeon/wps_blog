use std::fs;
use std::path::PathBuf;
use actix_web::{HttpServer, App, web::Data, Error, middleware::Logger, get, HttpResponse, HttpRequest};
use actix_files as actix_fs;
use clap::Parser;
use yew::ServerRenderer;
use shared::{ServerApp, ServerAppProps};

#[derive(Parser, Debug)]
struct Opt {
    #[clap(short, long)]
    dir: PathBuf,
}

#[get("/{tail:.*}")]
async fn render_yew_app(req: HttpRequest, static_dir: Data<PathBuf>) -> Result<HttpResponse, Error> {
    let index_html_s = fs::read_to_string(static_dir.join("index.html")).unwrap();
    let url = req.uri().to_string();

    let content = ServerRenderer
        ::<ServerApp>
        ::with_props(move || ServerAppProps {
            url: url.into(),
        }).render().await;

    Ok(HttpResponse::Ok()
        .content_type("text/html; charset=utf-8")
        .body(index_html_s.replace("<body>", &format!("<body>{}", content)))
    )
}

#[actix_web::main]
async fn main() -> std::io::Result<()> {
    std::env::set_var("RUST_LOG", "debug");
    std::env::set_var("RUST_BACKTRACE", "1");
    env_logger::init();

    HttpServer::new(|| {
        let logger = Logger::default();
        let opts = Opt::parse();

        let dir_data = Data::new(
            opts.dir.clone()
        );

        App::new()
            .wrap(logger)
            .app_data(dir_data)
            .service(
                actix_fs::Files::new("/static", opts.dir)
            )
            .service(render_yew_app)
    })
    .bind(("0.0.0.0", 8080))?
    .run()
    .await
}
