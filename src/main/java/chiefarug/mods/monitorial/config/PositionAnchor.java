package chiefarug.mods.monitorial.config;

import com.mojang.serialization.Codec;

import java.util.Locale;

import static chiefarug.mods.monitorial.config.PositionAnchor.Snap.*;

@SuppressWarnings("unused")
public enum PositionAnchor {
    TOP_LEFT(MIN, MIN), TOP_MIDDLE(MIN, MIDDLE), TOP_RIGHT(MIN, MAX),
    MIDDLE_LEFT(MIDDLE, MIN), CENTRE(MIDDLE, MIDDLE), MIDDLE_RIGHT(MIDDLE, MAX),
    BOTTOM_LEFT(MAX, MIN), BOTTOM_MIDDLE(MAX, MIDDLE), BOTTOM_RIGHT(MAX, MAX),
    DEFAULT(null, null);

    enum Snap {
        MIN {
            int get(int max) {
                return 0;
            }
        },
        MIDDLE {
            int get(int max) {
                return max / 2;
            }
        },
        MAX {
            int get(int max) {
                return max;
            }
        };

        abstract int get(int max);
    }

    private final Snap x, y;

    PositionAnchor(Snap x, Snap y) {
        this.x = x;
        this.y = y;
    }

    public int getAbsX(int minX, int maxX) {
        return minX + x.get(maxX - minX);
    }

    public int getAbsY(int minY, int maxY) {
        return minY + y.get(maxY - minY);
    }

    public static final Codec<PositionAnchor> CODEC = Codec.STRING.xmap(s -> PositionAnchor.valueOf(s.toUpperCase(Locale.ROOT)), Enum::name);
}
