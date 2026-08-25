package io.abbafather.domain.model

/**
 * The line of Scripture the home screen opens under the greeting. [reference] is set apart from
 * [text] because the design sets the verse italic and its reference as small meta type.
 */
data class DailyVerse(
    val text: String,
    val reference: String,
)

/**
 * The verses the home screen draws on, carried in the domain rather than in Room: they are fixed app
 * content the reader never edits, so there is nothing for a source of truth to be true about. Every
 * one is the Authorized Version, which is public domain.
 */
val DailyVerses: List<DailyVerse> = listOf(
    DailyVerse("Be still, and know that I am God.", "Psalm 46:10"),
    DailyVerse("The Lord is my shepherd; I shall not want.", "Psalm 23:1"),
    DailyVerse("Come unto me, all ye that labour and are heavy laden, and I will give you rest.", "Matthew 11:28"),
    DailyVerse("Cast all your care upon him; for he careth for you.", "1 Peter 5:7"),
    DailyVerse("The Lord is nigh unto them that are of a broken heart.", "Psalm 34:18"),
    DailyVerse("Thy word is a lamp unto my feet, and a light unto my path.", "Psalm 119:105"),
    DailyVerse("His compassions fail not. They are new every morning.", "Lamentations 3:22–23"),
    DailyVerse("Peace I leave with you, my peace I give unto you.", "John 14:27"),
    DailyVerse("Abide in me, and I in you.", "John 15:4"),
    DailyVerse("Create in me a clean heart, O God; and renew a right spirit within me.", "Psalm 51:10"),
    DailyVerse("They that wait upon the Lord shall renew their strength.", "Isaiah 40:31"),
    DailyVerse("In quietness and in confidence shall be your strength.", "Isaiah 30:15"),
    DailyVerse("Underneath are the everlasting arms.", "Deuteronomy 33:27"),
    DailyVerse("The Lord bless thee, and keep thee.", "Numbers 6:24"),
    DailyVerse("O taste and see that the Lord is good.", "Psalm 34:8"),
    DailyVerse("I will lift up mine eyes unto the hills, from whence cometh my help.", "Psalm 121:1"),
    DailyVerse("Ye have received the Spirit of adoption, whereby we cry, Abba, Father.", "Romans 8:15"),
    DailyVerse("Let not your heart be troubled, neither let it be afraid.", "John 14:27"),
    DailyVerse("This is the day which the Lord hath made; we will rejoice and be glad in it.", "Psalm 118:24"),
    DailyVerse("Draw nigh to God, and he will draw nigh to you.", "James 4:8"),
    DailyVerse("The eternal God is thy refuge.", "Deuteronomy 33:27"),
    DailyVerse("Pray without ceasing.", "1 Thessalonians 5:17"),
    DailyVerse("Thou wilt keep him in perfect peace, whose mind is stayed on thee.", "Isaiah 26:3"),
    DailyVerse("Whither shall I go from thy spirit? or whither shall I flee from thy presence?", "Psalm 139:7"),
    DailyVerse("Weeping may endure for a night, but joy cometh in the morning.", "Psalm 30:5"),
    DailyVerse("Let the words of my mouth, and the meditation of my heart, be acceptable in thy sight.", "Psalm 19:14"),
    DailyVerse("Ask, and it shall be given you; seek, and ye shall find.", "Matthew 7:7"),
    DailyVerse("The Lord is my light and my salvation; whom shall I fear?", "Psalm 27:1"),
    DailyVerse("Search me, O God, and know my heart.", "Psalm 139:23"),
    DailyVerse("Into thine hand I commit my spirit.", "Psalm 31:5"),
    DailyVerse("Bless the Lord, O my soul: and all that is within me, bless his holy name.", "Psalm 103:1"),
)
