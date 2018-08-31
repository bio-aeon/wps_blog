import Koa from 'koa';
import KoaRouter from 'koa-router';
import chalk from 'chalk';
import ip from 'ip';
import boxen from 'boxen';

const logServerStarted = (opt = {}) => {
  let message = chalk.green(`Running ${(opt.chalk || chalk.bold)(opt.type)} in ${chalk.bold(process.env.NODE_ENV)} mode\n\n`);

  const localURL = `http://${opt.host}:${opt.port}`;
  message += `- ${chalk.bold('Local:           ')} ${localURL}`;

  try {
    const url = `http://${ip.address()}:${opt.port}`;
    message += `\n- ${chalk.bold('On Your Network: ')} ${url}`;
  } catch (err) { /* ignore errors */
  }

  console.log(
    boxen(message, {
      padding: 1,
      borderColor: 'green',
      margin: 1,
    }),
  );
};

const HOST = '0.0.0.0';
const PORT = '8081';

const router = new KoaRouter()
  .get('/test', async ctx => {
    ctx.body = 'test resp';
  });

const app = new Koa()
  .use(router.routes())
  .use(router.allowedMethods());

app.listen({host: HOST, port: PORT}, () => {
  logServerStarted({
    type: 'server',
    host: HOST,
    port: PORT,
  });
});
