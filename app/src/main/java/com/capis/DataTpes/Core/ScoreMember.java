package com.capis.DataTpes.Core;

public record ScoreMember(double score, String member) implements Comparable<ScoreMember> {

    @Override
    public int compareTo(ScoreMember other) {
        int byScore = Double.compare(this.score, other.score);
        return byScore != 0 ? byScore : this.member.compareTo(other.member);
    }

}
