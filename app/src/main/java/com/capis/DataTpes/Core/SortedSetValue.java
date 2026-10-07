package com.capis.DataTpes.Core;

import java.util.SortedSet;
import java.util.TreeSet;

public final class SortedSetValue implements Value<SortedSet<ScoreMember>> {
    private SortedSet<ScoreMember> value;

    public SortedSetValue(SortedSet<ScoreMember> value) {
        this.value = new TreeSet<>(value);
    }

    public ScoreMember find(String member) {
        for (ScoreMember sm : value) {
            if (sm.member().equals(member)) {
                return sm;
            }
        }
        return null;
    }

    public boolean removeMember(String member){
        ScoreMember existing = find(member);
        return existing != null && value.remove(existing);
    }

    public SortedSet<ScoreMember> getValue() {
        return value;
    }

    public void setValue(SortedSet<ScoreMember> value) {
        this.value = value;
    }
    
}
