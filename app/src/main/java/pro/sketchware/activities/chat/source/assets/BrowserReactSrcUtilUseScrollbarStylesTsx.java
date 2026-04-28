package pro.sketchware.activities.chat.source.assets;

import pro.sketchware.activities.chat.source.SourceAsset;
import pro.sketchware.activities.chat.source.SourceChunks;

public final class BrowserReactSrcUtilUseScrollbarStylesTsx implements SourceAsset {
    public static final BrowserReactSrcUtilUseScrollbarStylesTsx INSTANCE = new BrowserReactSrcUtilUseScrollbarStylesTsx();

    private static final String[] CHUNKS = new String[] {
            "// Get rid of this as it was causing lag\n\n// import { useEffect } from 'react';\n\n// export const useScrollbarStyles = (containerRef: React.RefObject<HTMLDivElement | null>) => {\n// \tuseEffect(() => {\n// \t\tif (!containerRef.current) return;\n\n// \t\t// Create selector for specific overflow classes\n// \t\tconst overflowSelector = [\n// \t\t\t'[class*=\"overflow-auto\"]',\n// \t\t\t'[class*=\"overflow-x-auto\"]',\n// \t\t\t'[class*=\"overflow-y-auto\"]'\n// \t\t].join(',');\n\n// \t\t// Function to initialize scrollbar styles for elements\n// \t\tconst initializeScrollbarStyles = () => {\n// \t\t\t// Get all matching elements within the container, including the container itself\n// \t\t\tconst scrollElements = [\n// \t\t\t\t...(containerRef.current?.matches(overflowSelector) ? [containerRef.current] : []),\n// \t\t\t\t...Array.from(containerRef.current?.querySelectorAll(overflowSelector) || [])\n// \t\t\t];\n\n// \t\t\t// Apply basic styling to all elements\n// \t\t\tscrollElements.forEach(element => {\n// \t\t\t\telement.classList.add('void-scrollable-element');\n// \t\t\t});\n\n// \t\t\t// Only initialize fade effects for elements that haven't been initialized yet\n// \t\t\tscrollElements.forEach(element => {\n// \t\t\t\tif (!(element as any).__scrollbarCleanup) {\n// \t\t\t\t\tlet fadeTimeout: NodeJS.Timeout | null = null;\n// \t\t\t\t\tlet fadeInterval: NodeJS.Timeout | null = null;\n\n// \t\t\t\t\tconst fadeIn = () => {\n// \t\t\t\t\t\tif (fadeInterval) clearInterval(fadeInterval);\n\n// \t\t\t\t\t\tlet step = 0;\n// \t\t\t\t\t\tfadeInterval = setInterval(() => {\n// \t\t\t\t\t\t\tif (step <= 10) {\n// \t\t\t\t\t\t\t\telement.classList.remove(`show-scrollbar-${step - 1}`);\n// \t\t\t\t\t\t\t\telement.classList.add(`show-scrollbar-${step}`);\n// \t\t\t\t\t\t\t\tstep++;\n// \t\t\t\t\t\t\t} else {\n// \t\t\t\t\t\t\t\tclearInterval(fadeInterval!);\n// \t\t\t\t\t\t\t}\n// \t\t\t\t\t\t}, 10);\n// \t\t\t\t\t};\n\n// \t\t\t\t\tconst fadeOut = () => {\n// \t\t\t\t\t\tif (fadeInterval) clearInterval(fadeInterval);\n\n// \t\t\t\t\t\tlet step = 10;\n// \t\t\t\t\t\tfadeInterval = setInterval(() => {\n// \t\t\t\t\t\t\tif (step >= 0) {\n// \t\t\t\t\t\t\t\telement.classList.remove(`show-scrollbar-${step + 1}`);\n// \t\t\t\t\t\t\t\telement.classList.add(`show-scrollbar-${step}`);\n// \t\t\t\t\t\t\t\tstep--;\n// \t\t\t\t\t\t\t} else {\n// \t\t\t\t\t\t\t\tclearInterval(fadeInterval!);\n// \t\t\t\t\t\t\t}\n// \t\t\t\t\t\t}, 60);\n// \t\t\t\t\t};\n\n// \t\t\t\t\tconst onMouseEnter = () => {\n// \t\t\t\t\t\tif (fadeTimeout) clearTimeout(fadeTimeout);\n// \t\t\t\t\t\tif (fadeInterval) clearInterval(fadeInterval);\n// \t\t\t\t\t\tfadeIn();\n// \t\t\t\t\t};\n\n// \t\t\t\t\tconst onMouseLeave = () => {\n// \t\t\t\t\t\tif (fadeTimeout) clearTimeout(fadeTimeout);\n// \t\t\t\t\t\tfadeTimeout = setTimeout(() => {\n// \t\t\t\t\t\t\tfadeOut();\n// \t\t\t\t\t\t}, 10);\n// \t\t\t\t\t};\n\n// \t\t\t\t\telement.addEventListener('mouseenter', onMouseEnter);\n// \t\t\t\t\telement.addEventListener('mouseleave', onMouseLeave);\n\n// \t\t\t\t\t// Store cleanup function\n// \t\t\t\t\tconst cleanup = () => {\n// \t\t\t\t\t\telement.removeEventListener('mouseenter', onMouseEnter);\n// \t\t\t\t\t\telement.removeEventListener('mouseleave', onMouseLeave);\n// \t\t\t\t\t\tif (fadeTimeout) clearTimeout(fadeTimeout);\n// \t\t\t\t\t\tif (fadeInterval) clearInterval(fadeInterval);\n// \t\t\t\t\t\telement.classList.remove('void-scrollable-element');\n// \t\t\t\t\t\t// Remove any remaining show-scrollbar classes\n// \t\t\t\t\t\tfor (let i = 0; i <= 10; i++) {\n// \t\t\t\t\t\t\telement.classList.remove(`show-scrollbar-${i}`);\n// \t\t\t\t\t\t}\n// \t\t\t\t\t};\n\n// \t\t\t\t\t// Store the cleanup function on the element for later use\n// \t\t\t\t\t(element as any).__scrollbarCleanup = cleanup;\n// \t\t\t\t}\n// \t\t\t});\n// \t\t};\n\n// \t\t// Initialize for the first time\n// \t\tinitializeScrollbarStyles();\n\n// \t\t// Set up mutation observer to do the same\n// \t\tconst observer = new MutationObserver(() => {\n// \t\t\tinitializeScrollbarStyles();\n// \t\t});\n\n// \t\t// Start observing the container for child changes\n// \t\tobserver.observe(containerRef.current, {\n// \t\t\tchildList: true,\n// \t\t\tsubtree: true\n// \t\t});\n\n// \t\treturn () => {\n// \t\t\tobserver.disconnect();\n// \t\t\t// Your existing cleanup code...\n// \t\t\tif (containerRef.current) {\n// \t\t\t\tconst scrollElements = [\n// \t\t\t\t\t...(containerRef.current.matches(overflowSelector) ? [containerRef.current] : []),\n// \t\t\t\t\t...Array.from(containerRef.current.querySelectorAll(overflowSelector))\n// \t\t\t\t];\n// \t\t\t\tscrollElements.forEach(element => {\n// \t\t\t\t\tif ((element as any).__scrollbarCleanup) {\n// \t\t\t\t\t\t(element as any).__scrollbarCleanup();\n// \t\t\t\t\t}\n// \t\t\t\t});\n// \t\t\t}\n// \t\t};\n// \t}, [containerRef]);\n// };\n"
    };

    private BrowserReactSrcUtilUseScrollbarStylesTsx() {
    }

    @Override
    public String path() {
        return "browser/react/src/util/useScrollbarStyles.tsx";
    }

    @Override
    public String sha256() {
        return "3d60cfcc47caf9cf74e7f7ed63ff31307fbc95e8f4fd376afc2dde51b8aa4f20";
    }

    @Override
    public int originalByteLength() {
        return 4267;
    }

    @Override
    public String source() {
        return SourceChunks.join(CHUNKS);
    }
}
