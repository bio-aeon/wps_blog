FROM rust:1.92-bookworm AS builder

RUN apt-get update && apt-get install -y --no-install-recommends binaryen && \
    rm -rf /var/lib/apt/lists/* && \
    rustup target add wasm32-unknown-unknown && \
    cargo install cargo-leptos --locked

WORKDIR /app

COPY . .
RUN cargo leptos build --release

FROM debian:bookworm-slim

RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates && \
    rm -rf /var/lib/apt/lists/* && \
    groupadd -r appgroup && useradd -r -g appgroup -s /usr/sbin/nologin appuser

WORKDIR /app

COPY --from=builder /app/target/release/blog-ui ./
COPY --from=builder /app/target/site/ ./site/

RUN chown -R appuser:appgroup /app

USER appuser

ENV LEPTOS_SITE_ROOT=/app/site
ENV LEPTOS_SITE_ADDR=0.0.0.0:3000

EXPOSE 3000

ENTRYPOINT ["./blog-ui"]
