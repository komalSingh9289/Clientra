import * as TabsPrimitive from "@radix-ui/react-tabs";
import { cn } from "@/lib/utils";

export const Tabs = TabsPrimitive.Root;
/** Scrolls horizontally on narrow screens instead of wrapping */
export const TabsList = ({ className, ...p }) => (
  <TabsPrimitive.List className={cn("flex w-full gap-1 overflow-x-auto border-b", className)} {...p} />
);
export const TabsTrigger = ({ className, ...p }) => (
  <TabsPrimitive.Trigger
    className={cn(
      "-mb-px shrink-0 border-b-2 border-transparent px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring data-[state=active]:border-primary data-[state=active]:text-foreground",
      className
    )}
    {...p}
  />
);
export const TabsContent = ({ className, ...p }) => <TabsPrimitive.Content className={cn("pt-4 focus-visible:outline-none", className)} {...p} />;
