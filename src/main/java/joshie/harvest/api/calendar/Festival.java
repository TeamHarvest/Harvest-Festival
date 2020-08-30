package joshie.harvest.api.calendar;

import joshie.harvest.api.buildings.Building;
import joshie.harvest.api.core.Letter;
import joshie.harvest.api.knowledge.Note;
import joshie.harvest.api.quests.Quest;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static joshie.harvest.api.calendar.CalendarDate.DAYS_PER_SEASON;

public final class Festival implements CalendarEntry {
    public static final HashMap<ResourceLocation, Festival> REGISTRY = new HashMap<>();
    public static final Festival NONE = new Festival(new ResourceLocation("harvestfestival", "none"));
    private static final ItemStack CLOCK = new ItemStack(Items.CLOCK);
    private final ResourceLocation resource;
    private Building requirement;
    private boolean affectsGround;
    private ItemStack icon;
    private boolean shopsOpen;
    private boolean hidden;
    private int length;
    private Quest quest;
    private Letter letter;
    private Note note;

    public Festival(@Nonnull ResourceLocation resource) {
        this.resource = resource;
        this.length = 3;
        this.affectsGround = true;
        this.icon = CLOCK;
        REGISTRY.put(resource, this);
    }

    /** Set the icon for this festival
     *  @param stack    the representative icon **/
    public Festival setIcon(@Nonnull ItemStack stack) {
        this.icon = stack;
        if (this.note.getIcon() == Note.PAPER) {
            this.note.setIcon(stack);
        }

        return this;
    }

    /** The note that gets added when this festival is
     *  around for the first time */
    public Festival setNote(Note note) {
        this.note = note;
        return this;
    }

    /** The letter that gets sent to the town mailbox **/
    public Festival setLetter(Letter letter) {
        this.letter = letter;
        return this;
    }

    /** The quest that gets activated by this festival **/
    public Festival setQuest(Quest quest) {
        this.quest = quest;
        return this;
    }

    /** Call this to make shops open on this festival **/
    public Festival setShopsOpen() {
        this.shopsOpen = true;
        return this;
    }

    /** Hide this festival from the calendar **/
    public Festival setHidden() {
        this.hidden = true;
        return this;
    }

    /** Set the festival length
     *  @param length   the number of days the festival will stay up **/
    public Festival setLength(int length) {
        this.length = length;
        return this;
    }

    /** Call this to make this festival change the look of the festival grounds **/
    public Festival setNoBuilding() {
        this.affectsGround = false;
        return this;
    }

    /** Set a building requirement for this festival to take place
     *  @param building     the building to use as a requirement for this festival**/
    public Festival setRequirement(Building building) {
        this.requirement = building;
        return this;
    }

    /** Returns how many days this festival lasts **/
    public int getFestivalLength() {
        return (int)(((double)length / 30D) * DAYS_PER_SEASON);
    }

    public boolean isHidden() {
        return hidden;
    }

    @Nullable
    public Building getRequirement() {
        return requirement;
    }

    @Nullable
    public Note getNote() {
        return note;
    }

    @Nullable
    public Letter getLetter() {
        return letter;
    }

    @Nonnull
    public ResourceLocation getResource() {
        return resource;
    }

    @Nullable
    public Quest getQuest() {
        return quest;
    }

    @Override
    @Nonnull
    public ItemStack getStackRepresentation() {
        return icon;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void addTooltipForCalendarEntry(List<String> tooltip) {
        if (note != null) tooltip.add(note.getTitle());
        else tooltip.addAll(Arrays.asList(I18n.translateToLocal(resource.getNamespace() + ".festival." + resource.getPath().replace("_", ".") + ".tooltip.").split("\n")));
    }

    public boolean doShopsOpen() {
        return shopsOpen;
    }

    public boolean affectsFestivalGrounds() {
        return affectsGround;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof Festival)) return false;
        Festival festival = (Festival) o;
        return resource.equals(festival.resource);
    }

    @Override
    public int hashCode() {
        return resource.hashCode();
    }
}