/**
 * @testing-library/react with the app's i18n provider (Turkish messages, the default
 * locale). Tests import render & co. from here instead of "@testing-library/react".
 */
import {
  render as rtlRender,
  renderHook as rtlRenderHook,
  type RenderOptions,
} from "@testing-library/react";
import { NextIntlClientProvider } from "next-intl";
import type { ComponentType, ReactElement, ReactNode } from "react";
import { MESSAGES } from "@/messages";

export * from "@testing-library/react";

export function IntlWrapper({ children }: { children: ReactNode }) {
  return (
    <NextIntlClientProvider locale="tr" messages={MESSAGES.tr} timeZone="Europe/Istanbul">
      {children}
    </NextIntlClientProvider>
  );
}

function withIntl(Inner?: ComponentType<{ children: ReactNode }>) {
  return function Wrapper({ children }: { children: ReactNode }) {
    return <IntlWrapper>{Inner ? <Inner>{children}</Inner> : children}</IntlWrapper>;
  };
}

export function render(ui: ReactElement, options: RenderOptions = {}) {
  return rtlRender(ui, {
    ...options,
    wrapper: withIntl(options.wrapper as ComponentType<{ children: ReactNode }>),
  });
}

export function renderHook<Result, Props>(
  hook: (props: Props) => Result,
  options: { wrapper?: ComponentType<{ children: ReactNode }>; initialProps?: Props } = {},
) {
  return rtlRenderHook(hook, { ...options, wrapper: withIntl(options.wrapper) });
}
